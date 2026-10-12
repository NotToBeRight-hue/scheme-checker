import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { cleanup, fireEvent, render, screen } from "@testing-library/react";
import { api, type Resource } from "./api";
import { ExactJsonBody } from "./exact-json";
import { AdminRules } from "./main";

vi.mock("./api", async (importOriginal) => {
  const actual = await importOriginal<typeof import("./api")>();
  return { ...actual, api: vi.fn() };
});

const mockedApi = vi.mocked(api);
const ruleText = (value: string) => `{"schemaVersion":1,"type":"GROUP","operator":"AND","children":[{"type":"CONDITION","nodeId":"income","field":"annualIncome","operator":"EQ","value":${value}}]}`;
let versions: Resource[];

beforeEach(() => {
  versions = [];
  mockedApi.mockReset();
  mockedApi.mockImplementation(async (path, method = "GET", body) => {
    if (method === "GET") return versions;
    expect(body).toBeInstanceOf(ExactJsonBody);
    const json = (body as ExactJsonBody).json;
    const value = /"value":([0-9.]+)/.exec(json)?.[1];
    expect(value).toBeDefined();
    // Mimic the backend's exact decimal-string response. Backend/MySQL tests cover that conversion.
    const rule = JSON.parse(ruleText(JSON.stringify(value)));
    const saved = { id: "42", schemeId: "1", status: "DRAFT", revision: method === "POST" ? "0" : "1", rule };
    if (method === "PUT") expect(path).toBe("/admin/schemes/1/rules/42");
    versions = [saved];
    return saved;
  });
});
afterEach(cleanup);

describe("admin rule precision", () => {
  it("submits exact create/update values and retains them when retrieving the saved draft", async () => {
    render(<AdminRules scheme={{ id: "1" }} published={vi.fn()} />);
    fireEvent.change(screen.getByLabelText("Rule JSON"), { target: { value: ruleText("1000000000000000.11") } });
    fireEvent.click(screen.getByRole("button", { name: "Validate and save draft" }));
    const savedVersion = await screen.findByRole("button", { name: "Version 42: DRAFT, revision 0" });
    fireEvent.click(savedVersion);
    expect((screen.getByLabelText("Rule JSON") as HTMLTextAreaElement).value).toContain('"1000000000000000.11"');

    fireEvent.change(screen.getByLabelText("Rule JSON"), { target: { value: ruleText("1000000000000000.12") } });
    fireEvent.click(screen.getByRole("button", { name: "Validate and save draft" }));
    fireEvent.click(await screen.findByRole("button", { name: "Version 42: DRAFT, revision 1" }));
    const mutations = mockedApi.mock.calls.filter(([, method]) => method !== undefined && method !== "GET");
    expect(mutations).toHaveLength(2);
    expect((mutations[0][2] as ExactJsonBody).json).toContain('"value":1000000000000000.11');
    expect((mutations[1][2] as ExactJsonBody).json).toContain('"value":1000000000000000.12');
    expect((mutations[1][2] as ExactJsonBody).json).toContain('"expectedDraftRevision":"0"');
    expect((screen.getByLabelText("Rule JSON") as HTMLTextAreaElement).value).toContain('"1000000000000000.12"');
  });

  it("shows a validation error without submitting an unsupported numeric spelling", async () => {
    render(<AdminRules scheme={{ id: "1" }} published={vi.fn()} />);
    fireEvent.change(screen.getByLabelText("Rule JSON"), { target: { value: ruleText("Infinity") } });
    fireEvent.click(screen.getByRole("button", { name: "Validate and save draft" }));
    expect((await screen.findByRole("alert")).textContent).toContain("NaN and Infinity are not supported");
    expect(mockedApi.mock.calls.filter(([, method]) => method === "POST" || method === "PUT")).toHaveLength(0);
  });
});
