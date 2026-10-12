import type { Resource } from "./api";

export function displayText(value: unknown): string {
  return String(value ?? "").replace(/\u2014/g, ";");
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
