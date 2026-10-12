import type { Resource } from "./api";

export function displayText(value: unknown): string {
  return String(value ?? "")
    .replace(/\s*FICTIONAL \/ DEMO ONLY\. Not a government programme or available financial assistance\./g, "")
    .replace(/\s*[\u2014;] invented demonstration amount only\. No payout or entitlement\./g, "")
    .replace(/^FICTIONAL \/ DEMO ONLY\. /, "")
    .replace(/ No application or official verification occurs\./g, "")
    .replace(/ Fictional demonstration only\./g, "")
    .replace(/^Demo: /, "")
    .replace(/\u2014/g, ";");
}

export const schemeNames = ["Student Education Support", "Women Higher Education Grant", "Farmer Family Assistance", "Senior Citizen Welfare Support", "Youth Skill Development Grant", "Disability Education Assistance", "Small Business Startup Support", "School Excellence Scholarship"];

export function schemeDisplayName(value: unknown): string {
  const name = String(value ?? "");
  const clean = name.replace(/^FICTIONAL \/ DEMO ONLY: /, "");
  return schemeNames.includes(clean) ? clean : name;
}

export function pdfDemoSchemes(schemes: Resource[]): Resource[] {
  return schemes.filter(scheme => {
    try {
      const mapping = typeof scheme.sourceMapping === "string" ? JSON.parse(scheme.sourceMapping) : scheme.sourceMapping;
      return scheme.synthetic === true && mapping?.demoSpecification === "eight-demo-pdf-v1";
    } catch { return false; }
  });
}

export function matchingOverview(preview: Resource | null | undefined, revision: string, schemes: Resource[]) {
  if (!preview || String(preview.profileRevision) !== String(revision)) return null;
  const demos = pdfDemoSchemes(schemes);
  const rows: Resource[] = preview.results || [];
  if (demos.length !== 8 || rows.length !== demos.length || new Set(rows.map(row => row.schemeId)).size !== rows.length) return null;
  if (rows.some(row => !demos.some(scheme => scheme.id === row.schemeId && scheme.publishedVersionId === row.ruleVersionId) || !["ELIGIBLE", "NOT_ELIGIBLE", "NEEDS_INFORMATION"].includes(row.classification))) return null;
  return {
    eligible: rows.filter(row => row.classification === "ELIGIBLE").length,
    notEligible: rows.filter(row => row.classification === "NOT_ELIGIBLE").length,
    needsInformation: rows.filter(row => row.classification === "NEEDS_INFORMATION").length,
    total: rows.length,
  };
}
