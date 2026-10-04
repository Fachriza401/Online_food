#!/usr/bin/env node
// Generator placeholder SVG (tanpa watermark, tanpa foto eksternal).
// Foto asli berlisensi dapat menggantikan file ini — lihat docs/IMAGE_ASSETS.md.
import { mkdirSync, writeFileSync } from "node:fs";
import { dirname, join } from "node:path";
import { fileURLToPath } from "node:url";

const here = dirname(fileURLToPath(import.meta.url));
const base = join(here, "..", "src", "main", "resources", "static", "images");

const CAT_COLORS = {
  nasi: "#F97316",
  ayam: "#E2571B",
  sate: "#C2410C",
  lauk: "#D97706",
  minuman: "#0284C7",
  extra: "#16A34A",
};

function svgFood(name, cat, id, note) {
  const c = CAT_COLORS[cat] || CAT_COLORS.nasi;
  const gradId = "g" + (id || name.replace(/[^a-z0-9]/gi, ""));
  return `<svg xmlns="http://www.w3.org/2000/svg" width="800" height="600" viewBox="0 0 800 600" role="img" aria-labelledby="t${gradId}">
  <title id="t${gradId}">${name}</title>
  <defs>
    <linearGradient id="${gradId}" x1="0" y1="0" x2="1" y2="1">
      <stop offset="0" stop-color="#FFF9F3"/>
      <stop offset="1" stop-color="#FFEAD1"/>
    </linearGradient>
    <radialGradient id="${gradId}p" cx="0.5" cy="0.5" r="0.5">
      <stop offset="0" stop-color="${c}" stop-opacity="0.18"/>
      <stop offset="1" stop-color="${c}" stop-opacity="0.04"/>
    </radialGradient>
  </defs>
  <rect width="800" height="600" fill="url(#${gradId})"/>
  <circle cx="400" cy="255" r="300" fill="url(#${gradId}p)"/>
  <circle cx="150" cy="90" r="54" fill="${c}" opacity="0.12"/>
  <circle cx="700" cy="520" r="70" fill="${c}" opacity="0.10"/>
  <circle cx="400" cy="255" r="168" fill="#FFFFFF" opacity="0.85"/>
  <circle cx="400" cy="255" r="168" fill="none" stroke="${c}" stroke-width="3" stroke-dasharray="1 14" stroke-linecap="round" opacity="0.55"/>
  <path d="M400 120a135 135 0 1 1-119 74l36 44q-22 26-12 46 12 24 46 24 48 0 72-32t4-74z" fill="${c}" opacity="0.55"/>
  <path d="M362 128q38 34 76-8" stroke="#FFFFFF" stroke-width="8" fill="none" stroke-linecap="round" opacity="0.9"/>
  <text x="400" y="452" text-anchor="middle" font-family="Inter, 'Segoe UI', Arial, sans-serif" font-size="34" font-weight="700" fill="#292524">${name}</text>
  <text x="400" y="482" text-anchor="middle" font-family="Inter, 'Segoe UI', Arial, sans-serif" font-size="16" fill="#78716C">${note || "Placeholder foto sementara"}</text>
</svg>`;
}

function svgCat(name, c, id) {
  const gradId = "c" + id;
  return `<svg xmlns="http://www.w3.org/2000/svg" width="400" height="400" viewBox="0 0 400 400" role="img" aria-labelledby="t${gradId}">
  <title id="t${gradId}">${name}</title>
  <defs>
    <linearGradient id="${gradId}" x1="0" y1="0" x2="1" y2="1">
      <stop offset="0" stop-color="#FFF9F3"/>
      <stop offset="1" stop-color="#FFEAD1"/>
    </linearGradient>
  </defs>
  <rect width="400" height="400" fill="url(#${gradId})"/>
  <circle cx="200" cy="170" r="118" fill="#FFFFFF" opacity="0.9"/>
  <circle cx="200" cy="170" r="118" fill="none" stroke="${c}" stroke-width="3" stroke-dasharray="1 12" stroke-linecap="round" opacity="0.6"/>
  <path d="M200 86a84 84 0 1 1-74 46l22 27q-14 16-7 28 8 15 28 15 30 0 45-20t2-46z" fill="${c}" opacity="0.6"/>
  <path d="M174 90q24 21 47-5" stroke="#FFFFFF" stroke-width="6" fill="none" stroke-linecap="round" opacity="0.9"/>
  <text x="200" y="352" text-anchor="middle" font-family="Inter, 'Segoe UI', Arial, sans-serif" font-size="28" font-weight="700" fill="#292524">${name}</text>
</svg>`;
}

function svgHero() {
  return `<svg xmlns="http://www.w3.org/2000/svg" width="1200" height="1000" viewBox="0 0 1200 1000" role="img" aria-labelledby="th">
  <title id="th">Hidangan utama — placeholder hero</title>
  <defs>
    <linearGradient id="hg" x1="0" y1="0" x2="1" y2="1">
      <stop offset="0" stop-color="#FFF9F3"/>
      <stop offset="1" stop-color="#FFE3C3"/>
    </linearGradient>
    <radialGradient id="hp" cx="0.5" cy="0.5" r="0.5">
      <stop offset="0" stop-color="#F97316" stop-opacity="0.16"/>
      <stop offset="1" stop-color="#F97316" stop-opacity="0.03"/>
    </radialGradient>
  </defs>
  <rect width="1200" height="1000" fill="url(#hg)"/>
  <circle cx="600" cy="460" r="420" fill="url(#hp)"/>
  <circle cx="160" cy="180" r="90" fill="#F97316" opacity="0.10"/>
  <circle cx="1040" cy="820" r="120" fill="#C2410C" opacity="0.08"/>
  <circle cx="600" cy="460" r="300" fill="#FFFFFF" opacity="0.9"/>
  <circle cx="600" cy="460" r="300" fill="none" stroke="#F97316" stroke-width="3" stroke-dasharray="2 20" stroke-linecap="round" opacity="0.5"/>
  <path d="M600 230a230 230 0 1 1-203 126l60 75q-38 44-22 78 20 43 80 43 82 0 124-55t8-126z" fill="#F97316" opacity="0.6"/>
  <path d="M538 242q66 60 130-14" stroke="#FFFFFF" stroke-width="12" fill="none" stroke-linecap="round" opacity="0.95"/>
  <circle cx="600" cy="370" r="14" fill="#FFFFFF"/>
  <text x="600" y="868" text-anchor="middle" font-family="Inter, 'Segoe UI', Arial, sans-serif" font-size="52" font-weight="800" fill="#292524">Hidangan Utama Kami</text>
  <text x="600" y="912" text-anchor="middle" font-family="Inter, 'Segoe UI', Arial, sans-serif" font-size="22" fill="#78716C">Foto asli berlisensi akan menggantikan placeholder ini</text>
</svg>`;
}

const foods = [
  ["Nasi Goreng Spesial", "nasi"], ["Nasi Goreng Ayam", "nasi"], ["Nasi Uduk Komplit", "nasi"],
  ["Ayam Bakar", "ayam"], ["Ayam Goreng Krispi", "ayam"], ["Ayam Geprek", "ayam"],
  ["Sate Ayam", "sate"], ["Sate Kambing", "sate"], ["Ikan Bakar", "sate"],
  ["Telur Balado", "lauk"], ["Tempe & Tahu", "lauk"],
  ["Es Teh Manis", "minuman"], ["Es Jeruk Peras", "minuman"], ["Jus Alpukat", "minuman"],
];
const cat = {
  nasi: { name: "Nasi", color: "#F97316" },
  ayam: { name: "Ayam", color: "#E2571B" },
  "sate-bakar": { name: "Sate & Bakar", color: "#C2410C" },
  lauk: { name: "Lauk Sampingan", color: "#D97706" },
  minuman: { name: "Minuman", color: "#0284C7" },
};

let files = {};

foods.forEach(([name, catId], i) => {
  const slug = name.toLowerCase().replace(/[^a-z0-9 ]/g, "").replace(/ +/g, "-");
  const note = catId === "minuman" ? "Minuman Segar" : catId === "nasi" ? "Hidangan Nasi" : "Hidangan";
  files[join(base, "foods", slug + ".svg")] = svgFood(name, catId, "f" + i, note);
});

Object.entries(cat).forEach(([key, v], i) => {
  const slug = key === "sate-bakar" ? "sate-bakar" : key;
  files[join(base, "categories", slug + ".svg")] = svgCat(v.name, v.color, "c" + i);
});

files[join(base, "hero", "hero.svg")] = svgHero();
files[join(base, "placeholders", "placeholder-food.svg")] = svgFood("Menu", "nasi", "ph", "Foto asli menyusul");
files[join(base, "placeholders", "placeholder-category.svg")] = svgCat("Kategori", "#D97706", "phc");

// Logo & favicon
files[join(base, "branding", "logo.svg")] = `<svg xmlns="http://www.w3.org/2000/svg" width="120" height="120" viewBox="0 0 120 120" role="img" aria-labelledby="tl">
  <title id="tl">Logo Toko Kita</title>
  <rect width="120" height="120" rx="26" fill="#F97316"/>
  <path d="M60 20a40 40 0 1 1-35 23l10 13q-7 8-4 14 4 9 14 9 14 0 21-10t1-22z" fill="#FFFFFF"/>
  <path d="M52 22q14 12 26-2" stroke="#F97316" stroke-width="5" fill="none" stroke-linecap="round"/>
  <circle cx="60" cy="56" r="5" fill="#F97316"/>
</svg>`;
files[join(base, "branding", "favicon.svg")] = `<svg xmlns="http://www.w3.org/2000/svg" width="64" height="64" viewBox="0 0 64 64">
  <rect width="64" height="64" rx="14" fill="#F97316"/>
  <path d="M32 10a22 22 0 1 1-19 13l5 7q-4 4-2 7 2 5 8 5 8 0 12-6t0-12z" fill="#FFFFFF"/>
  <circle cx="32" cy="30" r="3" fill="#F97316"/>
</svg>`;

for (const [path, content] of Object.entries(files)) {
  mkdirSync(dirname(path), { recursive: true });
  writeFileSync(path, content, "utf8");
}
console.log("Generated " + Object.keys(files).length + " placeholder assets under " + join(base, "..", "images"));