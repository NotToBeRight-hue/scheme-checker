import { describe, expect, it } from "vitest";
import { ExactJsonBody, exactRuleBody } from "./exact-json";

describe("exact rule JSON transport", () => {
  it("retains a large decimal token and the existing request shape", () => {
    const rule = '{"schemaVersion":1,"type":"GROUP","operator":"AND","children":[{"type":"CONDITION","nodeId":"income","field":"annualIncome","operator":"LTE","value":1000000000000000.11}]}';
    const body = exactRuleBody(rule, "7");
    expect(body.json).toBe(`{"rule":${rule},"expectedDraftRevision":"7"}`);
    expect(body.json).toContain("1000000000000000.11");
  });

  it("preserves quoted decimals and numeric range/list tokens", () => {
    for (const value of ['"1000000000000000.1100"', '[1000000000000000.11,1000000000000000.12]', '[0.1,2.5e3]']) {
      const rule = `{"value":${value}}`;
      expect(exactRuleBody(rule).json).toBe(`{"rule":${rule}}`);
    }
  });

  it("retains safe numbers, booleans and strings without changing their types", () => {
    const json = '{"schemaVersion":1,"value":18,"fraction":0.25,"active":true,"label":"value:1000000000000000.11"}';
    expect(new ExactJsonBody(json).json).toBe(json);
    expect(JSON.parse(exactRuleBody(json).json).rule).toEqual(JSON.parse(json));
  });

  it("rejects invalid JSON and nonfinite number spellings clearly", () => {
    for (const value of ["NaN", "Infinity", "-Infinity"]) {
      expect(() => exactRuleBody(`{"value":${value}}`)).toThrow("NaN and Infinity are not supported");
    }
    expect(() => exactRuleBody('{"value":1,}')).toThrow("Invalid JSON");
    expect(() => exactRuleBody('{},"expectedDraftRevision":"9"')).toThrow("Invalid JSON");
  });
});
