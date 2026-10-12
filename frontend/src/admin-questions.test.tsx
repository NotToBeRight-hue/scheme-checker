import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { cleanup, fireEvent, render, screen, waitFor } from "@testing-library/react";
import { api, ApiError, type Resource } from "./api";
import { AdminQuestions } from "./main";

vi.mock("./api", async (importOriginal) => {
  const actual = await importOriginal<typeof import("./api")>();
  return { ...actual, api: vi.fn() };
});

const mockedApi = vi.mocked(api);
let questions: Resource[];

function mutations() {
  return mockedApi.mock.calls.filter(([, method]) => method === "POST" || method === "PUT");
}

function existingQuestion(): Resource {
  return { id: "42", label: "Existing question", type: "TEXT", options: [], active: true, revision: "3" };
}

async function selectExistingQuestion() {
  questions.push(existingQuestion());
  render(<AdminQuestions />);
  fireEvent.click(await screen.findByRole("button", { name: "42: Existing question" }));
}

async function saveLabel(label: string, expectedCalls: number) {
  fireEvent.change(screen.getByLabelText("Question label"), { target: { value: label } });
  fireEvent.click(screen.getByRole("button", { name: "Save question" }));
  await waitFor(() => expect(mutations()).toHaveLength(expectedCalls));
  // Every successful mutation reloads the list; wait until that completes before saving again.
  await screen.findByRole("button", { name: `42: ${label}` });
}

beforeEach(() => {
  questions = [];
  mockedApi.mockReset();
  mockedApi.mockImplementation(async (path, method = "GET", body) => {
    if (method === "GET" && path === "/admin/questions") {
      return questions.map((question) => ({ ...question, options: [...question.options] }));
    }
    const payload = body as Resource;
    if (method === "POST" && path === "/admin/questions") {
      const created: Resource = { ...payload, id: String(42 + questions.length), revision: "0" };
      delete created.expectedRevision;
      questions.push(created);
      return { ...created };
    }
    if (method === "PUT") {
      const index = questions.findIndex((question) => path === `/admin/questions/${question.id}`);
      if (index !== -1) {
        const current = questions[index];
        if (payload.expectedRevision !== current.revision) {
          throw new ApiError(409, "STALE_REVISION", "Data changed. Reload before submitting.");
        }
        const updated: Resource = { ...payload, id: current.id, revision: String(Number(current.revision) + 1) };
        delete updated.expectedRevision;
        questions[index] = updated;
        return { ...updated };
      }
    }
    throw new Error(`Unexpected request: ${method} ${path}`);
  });
});

afterEach(cleanup);

describe("admin question save state", () => {
  it("creates once and updates the returned question on another save without reselecting", async () => {
    render(<AdminQuestions />);
    await saveLabel("New question", 1);
    await saveLabel("Revised question", 2);

    expect(mutations().map(([path, method]) => [path, method])).toEqual([
      ["/admin/questions", "POST"],
      ["/admin/questions/42", "PUT"],
    ]);
    expect((mutations()[1][2] as Resource).expectedRevision).toBe("0");
    expect(questions).toHaveLength(1);
    expect(questions[0]).toMatchObject({ id: "42", label: "Revised question", revision: "1" });
  });

  it("uses each returned revision for consecutive edits without reselecting", async () => {
    await selectExistingQuestion();
    await saveLabel("First edit", 1);
    await saveLabel("Second edit", 2);
    await saveLabel("Third edit", 3);

    expect(mutations().map(([path, method]) => [path, method])).toEqual([
      ["/admin/questions/42", "PUT"],
      ["/admin/questions/42", "PUT"],
      ["/admin/questions/42", "PUT"],
    ]);
    expect(mutations().map(([, , body]) => (body as Resource).expectedRevision)).toEqual(["3", "4", "5"]);
    expect(questions).toHaveLength(1);
    expect(questions[0]).toMatchObject({ id: "42", label: "Third edit", revision: "6" });
  });

  it("preserves a genuine stale-revision conflict without overwriting or retrying", async () => {
    await selectExistingQuestion();
    questions[0] = { ...questions[0], label: "Other administrator's edit", revision: "4" };
    fireEvent.change(screen.getByLabelText("Question label"), { target: { value: "My unsaved edit" } });
    fireEvent.click(screen.getByRole("button", { name: "Save question" }));

    const alert = await screen.findByRole("alert");
    expect(alert.textContent).toContain("Data changed. Reload before submitting.");
    expect(alert.textContent).toContain("Reload this screen and review the latest values before submitting again.");
    expect(mutations()).toHaveLength(1);
    expect((mutations()[0][2] as Resource).expectedRevision).toBe("3");
    expect(questions[0]).toMatchObject({ label: "Other administrator's edit", revision: "4" });
    expect((screen.getByLabelText("Question label") as HTMLInputElement).value).toBe("My unsaved edit");
    expect(screen.queryByText("Question saved.")).toBeNull();
  });
});
