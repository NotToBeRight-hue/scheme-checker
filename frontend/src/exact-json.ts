// Validate syntax without replacing the original numeric JSON tokens with JS numbers.
export class ExactJsonBody {
  constructor(public readonly json: string) {
    try {
      JSON.parse(json);
    } catch {
      throw new Error("Invalid JSON. Use JSON numbers or quoted decimal strings; NaN and Infinity are not supported.");
    }
  }
}

export function exactRuleBody(rule: string, expectedDraftRevision?: string): ExactJsonBody {
  // Validate the editor as one JSON value before embedding it in the request.
  new ExactJsonBody(rule);
  const revision = expectedDraftRevision === undefined
    ? ""
    : `,"expectedDraftRevision":${JSON.stringify(expectedDraftRevision)}`;
  return new ExactJsonBody(`{"rule":${rule}${revision}}`);
}
