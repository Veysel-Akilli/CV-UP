import json
from typing import Dict, Any, Optional, List
import httpx
from app.core.config import settings

GEMINI_API_URL = (
    "https://generativelanguage.googleapis.com/v1/models/"
    "gemini-1.5-pro-002:generateContent"
)

class GeminiService:
    @staticmethod
    def _safe_str(v: Optional[str]) -> str:
        return v or ""

    @staticmethod
    def _compact_json(data: Dict[str, Any]) -> str:
        try:
            return json.dumps(data or {}, ensure_ascii=False, indent=2)
        except Exception:
            return str(data or {})

    @staticmethod
    def _has_text(d: Dict[str, Any], key: str) -> bool:
        v = GeminiService._safe_str(d.get(key)).strip()
        return len(v) > 0

    @staticmethod
    def _has_list(d: Dict[str, Any], key: str) -> bool:
        raw = GeminiService._safe_str(d.get(key))
        if not raw.strip():
            return False
        parts = [p.strip() for p in raw.replace("<|row|>", "\n").split("\n")]
        return any(p for p in parts if p)

    @staticmethod
    def _section_presence(data: Dict[str, Any]) -> Dict[str, bool]:
        """Girdide hangi bölümlerin veri içerdiğini belirler."""
        return {
            "AD SOYAD": GeminiService._has_text(data, "first_name") or GeminiService._has_text(data, "last_name") or GeminiService._has_text(data, "isim"),
            "İLETİŞİM": (
                any([
                    GeminiService._has_text(data, "email"),
                    GeminiService._has_text(data, "phone_num"),
                    GeminiService._has_text(data, "address"),
                    GeminiService._has_text(data, "linkedin"),
                    GeminiService._has_text(data, "github"),
                    GeminiService._has_text(data, "website"),
                ])
            ),
            "ÖZET": GeminiService._has_text(data, "objective") or GeminiService._has_text(data, "cv_summary"),
            "TEKNİK YETKİNLİKLER": GeminiService._has_list(data, "skills_list"),
            "DENEYİM": GeminiService._has_list(data, "experience_list"),
            "PROJELER": GeminiService._has_list(data, "projects_list"),
            "EĞİTİM": GeminiService._has_list(data, "education_list"),
            "DİLLER": GeminiService._has_list(data, "languages_list"),
            "SERTİFİKALAR/KURSLAR": GeminiService._has_list(data, "courses_list"),
            "REFERANSLAR": GeminiService._has_list(data, "references_list"),
        }

    @staticmethod
    def build_prompt(document_type: str, input_data: Dict[str, Any]) -> str:
        """
        Profesyonel, deterministik ve opsiyoneller boşken bile sağlam prompt.
        - Audience: ATS + teknik işe alım
        - Dil: Türkçe, sade; ölçülebilir etki
        - ‘|’ yasak; sadece düz metin; tek sütun
        """
        dt = (document_type or "").strip().lower()
        input_str = GeminiService._compact_json(input_data)

        if dt == "objective":
            profession = GeminiService._safe_str(input_data.get("profession"))
            goals = GeminiService._safe_str(input_data.get("goals"))
            if not (profession or goals or input_data):
                return (
                    "Objective için 2–3 cümle yaz. Birinci tekil (ben) kullanma; hedef rol, iş değeri ve teknoloji net olsun. "
                    "Abartı yok; ölçülebilir/denetlenebilir ifadeler kullan. ÇIKTI: yalın düz metin."
                )
            return (
                "Aşağıdaki girdiye göre 2–3 cümlelik, ATS uyumlu Objective yaz. "
                "Birinci tekil (ben) kullanma; hedef rol + beklenen etki + ilgili teknolojiler net olsun. "
                "Abartısız ve doğrulanabilir yaz.\n\n"
                f"Girdi JSON:\n{input_str}\n\n"
                "ÇIKTI: yalnız düz metin (madde işareti/başlık/markdown yok)."
            )

        if dt == "cv_summary":
            if not input_data:
                return (
                    "3–4 cümlede ATS uyumlu ÖZET yaz. Birinci tekil kullanma. "
                    "Kapsam (alan/ürün), sorumluluk (rol), teknoloji ve ölçülebilir etkiyi dengeli ver. "
                    "ÇIKTI: yalın düz metin."
                )
            return (
                "Aşağıdaki girdiye göre 3–4 cümlede ATS uyumlu ÖZET yaz. "
                "Birinci tekil kullanma; teknoloji/rol/etki üçlüsünü aktar, abartıdan kaçın.\n\n"
                f"Girdi JSON:\n{input_str}\n\n"
                "ÇIKTI: sadece düz metin; madde işareti/başlık yok."
            )

        presence = GeminiService._section_presence(input_data)
        ordered = [
            "AD SOYAD", "İLETİŞİM", "ÖZET", "TEKNİK YETKİNLİKLER",
            "DENEYİM", "PROJELER", "EĞİTİM", "DİLLER",
            "SERTİFİKALAR/KURSLAR", "REFERANSLAR"
        ]
        include_sections = [s for s in ordered if presence.get(s)]
        skip_sections = [s for s in ordered if not presence.get(s)]

        has_phone = GeminiService._has_text(input_data, "phone_num")
        has_mail = GeminiService._has_text(input_data, "email")
        has_city = GeminiService._has_text(input_data, "address")
        has_linkedin = GeminiService._has_text(input_data, "linkedin")
        has_github = GeminiService._has_text(input_data, "github")
        has_web = GeminiService._has_text(input_data, "website")

        contact_policy = " · ".join([
            "Telefon: …" if has_phone else "",
            "E-posta: …" if has_mail else "",
            "Şehir: …" if has_city else "",
            "LinkedIn: …" if has_linkedin else "",
            "GitHub: …" if has_github else "",
            "Web: …" if has_web else "",
        ]).replace("  ", " ").strip(" ·")

        base_rules = (
            "Uzman bir ATS danışmanı gibi davran. Türkçe, profesyonel ve sade üslupla, TEK SÜTUN ve YALNIZ DÜZ METİN bir CV üret. "
            "Kod bloğu, markdown başlıkları (#, **), tablo, emoji, ikon KULLANMA. Dikey çizgi '|' KESİNLİKLE KULLANMA.\n\n"
            "BÖLÜM SIRASI (yalnız aşağıda 'YAZILACAK' listesinde olanları yaz):\n"
            "AD SOYAD\nİLETİŞİM\nÖZET\nTEKNİK YETKİNLİKLER\nDENEYİM\nPROJELER\nEĞİTİM\nDİLLER\nSERTİFİKALAR/KURSLAR\nREFERANSLAR\n\n"
            "BÖLÜM POLİTİKASI:\n"
            f"- YAZILACAK: {', '.join(include_sections) if include_sections else '(yok)'}\n"
            f"- YAZILMAYACAK: {', '.join(skip_sections) if skip_sections else '(yok)'}\n"
            "- ‘YAZILMAYACAK’ listedekiler için asla başlık, boş satır, 'Talep üzerine...' veya 'N/A' yazma.\n\n"
            "FORMAT KURALLARI:\n"
            "- Bölümler arasında tam 1 boş satır.\n"
            "- Madde işaretleri satır başında: '- ' (başında boşluk yok).\n"
            "- Tarih biçimi: 'YYYY-MM'. Devam eden için bitişe 'Devam' yaz.\n"
            "- Birinci tekil (ben) kullanma; kısa, net ve ölçülebilir etki ver.\n"
            "- Ayrım için ‘—’ (uzun tire) veya ‘·’ kullan; asla '|' kullanma.\n\n"
            "BÖLÜM ÖRÜNTÜLERİ:\n"
            f"- İLETİŞİM: {contact_policy if contact_policy else '(girdi yoksa bu bölümü yazma)'}\n"
            "- DENEYİM: İlk satır 'Şirket — Rol — Konum — Başlangıç: YYYY-MM — Bitiş: YYYY-MM/Devam'. "
            "Altında 2–5 madde: aksiyon fiili + çıktı/etki + metrik (%/ms/↑/↓/x).\n"
            "- PROJELER: 'Proje Adı — (Teknolojiler) — Yıl' ve 1–2 madde.\n"
            "- EĞİTİM: 'Üniversite — Bölüm (Lisans/YL) — Konum — YYYY-YYYY — GPA (varsa)'.\n"
            "- DİLLER: Her satır '- Dil (Seviye)'.\n"
            "- REFERANSLAR: 'Ad Soyad — İlişki, Pozisyon · Şirket/Şehir · E-posta · Telefon'. Referans yoksa bu bölümü yazma.\n\n"
            "KALİTE:\n"
            "- 450–600 kelime hedefle; tekrar ve süslü ifadelerden kaçın.\n"
            "- Android/Kotlin/MVVM/Compose, Retrofit, Coroutines, Room, Firebase, FastAPI, PostgreSQL, Docker, JWT vb. "
            "yalnız ilgili yerlerde doğal biçimde geçsin.\n\n"
        )

        if not input_data:
            return (
                base_rules
                + "Girdi JSON: {}\n\n"
                "Girdi boşsa yalnız AD SOYAD/ÖZET/TEKNİK YETKİNLİKLER için nötr bir iskelet yaz, diğer tüm bölümleri atla. "
                "Varsayım yapma; genel ama gerçekçi ifadeler kullan. ÇIKTI: yalın düz metin."
            )

        return (
            base_rules
            + f"Girdi JSON:\n{input_str}\n\n"
            "Şimdi YALNIZCA düz metin olarak CV’yi üret. Başlık işaretleri, kod bloğu, alıntı, BEGIN/END veya '|' kullanma."
        )

    @staticmethod
    def _extract_text_parts(data: Dict[str, Any]) -> str:
        texts: List[str] = []
        try:
            for cand in (data.get("candidates") or []):
                content = (cand or {}).get("content") or {}
                for p in content.get("parts") or []:
                    t = (p or {}).get("text")
                    if isinstance(t, str) and t.strip():
                        texts.append(t)
        except Exception:
            pass
        return "\n".join(texts).strip()

    @staticmethod
    async def generate_content(document_type: str, input_data: Dict[str, Any]) -> str:
        try:
            prompt = GeminiService._safe_str(
                GeminiService.build_prompt(document_type, input_data or {})
            ).strip()
            if not prompt:
                prompt = (
                    "ATS uyumlu, tek sütun ve yalnızca düz metin bir CV üret. "
                    "Birinci tekil kullanma; '- ' madde stili ve 'YYYY-MM' tarih biçimi uygula; '|' kullanma."
                )

            headers = {"Content-Type": "application/json"}
            params = {"key": settings.GEMINI_API_KEY}

            payload = {
                "contents": [
                    {"role": "user", "parts": [{"text": prompt}]}
                ],
                "generationConfig": {
                    "temperature": 0.3,
                    "topP": 0.95,
                    "topK": 32,
                    "maxOutputTokens": 1200
                }
            }

            async with httpx.AsyncClient() as client:
                resp = await client.post(
                    GEMINI_API_URL,
                    headers=headers,
                    params=params,
                    json=payload,
                    timeout=90,
                )

            if resp.status_code != 200:
                return f"Gemini API Hatası: {resp.status_code} - {resp.text}"

            data = resp.json()
            text = GeminiService._extract_text_parts(data)
            return text or "Gemini API'den beklenmeyen yanıt formatı."
        except httpx.HTTPStatusError as e:
            try:
                return f"Gemini API HTTP Hatası: {e.response.status_code} - {e.response.text}"
            except Exception:
                return f"Gemini API HTTP Hatası: {str(e)}"
        except httpx.RequestError as e:
            return f"Gemini API Bağlantı Hatası: {str(e)}"
        except Exception as e:
            return f"Beklenmeyen Hata: {str(e)}"
