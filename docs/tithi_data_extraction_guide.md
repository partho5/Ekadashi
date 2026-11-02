# Panjika Tithi Data Extraction Guide

This document describes how to fetch, parse, and structure daily Tithi data matching traditional Bengali Panjikas (such as **Nabajug Half Panjika**) using `ponjika.com`.

---

## 1. Background & Why This Source Works
Traditional Bengali Panjikas in Bangladesh and West Bengal (Nabajug, Gupta Press, Benimadhav Shil) follow **Surya Siddhanta** (সূর্যসিদ্ধান্ত) calculations rather than modern Drik Ephemeris. 

`ponjika.com` computes traditional Surya Siddhanta Panchanga dynamically with configurable geographic coordinates and regional timezones.

---

## 2. Endpoint & Query Parameters

The service accepts GET requests with explicit query parameters:

```
https://ponjika.com/Default.aspx?date={YYYY-MM-DD}&lat={LAT}&lon={LON}&tz={TIMEZONE}
```

### Parameters:
| Parameter | Description | Recommended for Bangladesh | Recommended for Kolkata / WB |
| :--- | :--- | :--- | :--- |
| `date` | Target date (`YYYY-MM-DD`) | e.g. `2026-11-05` | e.g. `2026-11-05` |
| `lat` | Latitude of observer | `23.8103` (Dhaka) | `22.5833` (Kolkata) |
| `lon` | Longitude of observer | `90.4125` (Dhaka) | `88.3767` (Kolkata) |
| `tz` | Timezone identifier | `Bangladesh+Standard+Time` | `India+Standard+Time` |

---

## 3. HTML DOM Structure & Parsing

Each day's astrological elements are rendered in `.panchanga-line` `<div>` tags.

### Example Raw DOM Element:
```html
<div class="panchanga-line">
  <b>কৃষ্ণপক্ষ | তিথি:</b> একাদশী (নন্দা) অপরাহ্ন ঘ ১২:২১:৩১ দং ১৫/২৮/৮.৬০ পর্যন্ত পরে দ্বাদশী
</div>
```

### Field Breakdown:
* **Paksha (পক্ষ):** `কৃষ্ণপক্ষ` or `শুক্লপক্ষ`
* **Tithi Name (তিথি):** `একাদশী` *(sub-classification like `(নন্দা)` is removed for pure tithi names)*
* **Time Period:** `অপরাহ্ন` (Afternoon), `সন্ধ্যা` (Evening), `সকাল` (Morning), `প্রাত` (Dawn), `রাত্র` (Night), or `নিশা` (Late night / past midnight)
* **Time (সময়):** `ঘ ১২:২১:৩১` (Bengali numerals)
* **Danda-Pala (দণ্ড-পল):** `দং ১৫/২৮/৮.৬০`
* **Next Tithi (পরবর্তী তিথি):** `পরে দ্বাদশী`

---

## 4. Normalization Rules

### A. Bengali Digits Conversion
Bengali numerals (`০১২৩৪৫৬৭৮৯`) map 1:1 to ASCII digits (`0123456789`).

### B. Time Conversion to 24-Hour Format (`YYYY-MM-DD HH:MM:SS`)
* `সকাল` / `প্রাত`: Morning AM (`06:00` – `11:59`). If 12, converted to `00`.
* `অপরাহ্ন` / `দিবা` / `সন্ধ্যা` / `রাত্র`: PM (`12:00` – `23:59`). Hours 1–11 add `+12`.
* `নিশা`: Late night past midnight. In Panjika, `নিশা ঘ ১২:২০` belongs to the night of the solar day, representing `00:20:00` of the **next calendar morning** (`+1 day`).

### C. Calculating Start and End Timestamps
Every Tithi's transition is continuous:
* **`end` of Tithi $N$:** The timestamp calculated directly from day $N$'s record.
* **`start` of Tithi $N$:** The exact `end` timestamp of the preceding Tithi $N-1$.
*(Always fetch 1 day before the target start date to determine the initial Tithi's start time).*

---

## 5. Output JSON Schema

The extracted dataset includes a top-level `timezone` key, geographic metadata, and a clean array of `tithis`:

```json
{
  "timezone": "Bangladesh Standard Time (UTC+06:00)",
  "latitude": 23.8103,
  "longitude": 90.4125,
  "source": "https://ponjika.com",
  "generated_at": "2026-11-02",
  "tithis": [
    {
      "tithi": "সপ্তমী",
      "start": "2026-10-31 21:56:52",
      "end": "2026-11-01 19:36:49"
    },
    {
      "tithi": "অষ্টমী",
      "start": "2026-11-01 19:36:49",
      "end": "2026-11-02 17:24:40"
    }
  ]
}
```

---

## 6. Python Automation Script

To regenerate or extend the dataset for any duration, run:

```bash
python3 scripts/scrape_tithis.py
```

*(See `scripts/scrape_tithis.py` in the project repository for the full standalone scraper script).*
