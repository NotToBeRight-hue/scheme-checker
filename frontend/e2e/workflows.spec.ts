import { test, expect } from "@playwright/test";
import { readFileSync } from "node:fs";
const env = Object.fromEntries(
  readFileSync("../.env", "utf8")
    .split(/\r?\n/)
    .filter((x) => x.includes("="))
    .map((x) => [x.slice(0, x.indexOf("=")), x.slice(x.indexOf("=") + 1)]),
);
test("citizen registration, profile, discovery, explanations, history, suggestions and logout", async ({
  page,
  context,
}) => {
  const errors: string[] = [];
  page.on("pageerror", (e) => errors.push(e.message));
  page.on("console", m=>{if(m.type()==="error")errors.push(m.text());});
  await page.goto("/");
  await page.getByRole("button", { name: "Create a citizen account" }).click();
  await page.getByLabel("Full name").fill("Browser Citizen");
  await page
    .getByLabel("Email", { exact: true })
    .fill("browser-" + Date.now() + "@example.test");
  await page.getByLabel("Password", { exact: true }).fill("browserpassword123");
  await page
    .getByRole("button", { name: "Create account", exact: true })
    .click();
  await expect(
    page.getByRole("heading", { name: "Welcome, Browser Citizen" }),
  ).toBeVisible();
  const cookie = (await context.cookies()).find((c) => c.name === "SESSION");
  expect(cookie?.httpOnly).toBe(true);
  expect(cookie?.path).toBe("/api");
  expect(cookie?.sameSite).toBe("Lax");
  await page.getByRole("link", { name: "My profile", exact: true }).click();
  await page.getByLabel("Date of birth").fill("2000-01-01");
  await page.getByLabel("State or union territory").selectOption("MH");
  await page.getByLabel("Annual income").fill("100.00");
  await page.getByLabel("Education level").selectOption("GRADUATE");
  await page.getByLabel("Occupation").fill("STUDENT");
  await page.getByRole("button", { name: "Save profile", exact: true }).click();
  await expect(
    page.getByRole("status").filter({ hasText: "Profile saved" }),
  ).toBeVisible();
  await page.getByRole("link", { name: "Browse schemes", exact: true }).click();

  await page.getByLabel("Search schemes").fill("Student Education Support");
  await page.getByRole("link", { name: "View scheme", exact: true }).click();
  await page.getByLabel("Age (integer years)").fill("25");
  await page.getByLabel("State of residence").selectOption("TN");
  await page.getByLabel("Currently enrolled in college").selectOption("true");
  await page.getByLabel("Annual family income (decimal INR)").fill("300000");
  await page.getByRole("button", { name: "Save answers", exact: true }).click();
  await expect(
    page.getByRole("status").filter({ hasText: "Answers saved" }),
  ).toBeVisible();
  await page
    .getByRole("button", { name: "Check eligibility", exact: true })
    .click();
  await expect(
    page.getByRole("heading", { name: "Eligibility explanation" }),
  ).toBeVisible();
  await expect(page.locator("tbody tr")).toHaveCount(4);
  await expect(page.getByText("ELIGIBLE", { exact: true })).toBeVisible();
  await page.getByRole("link", { name: "Check history", exact: true }).click();
  await expect(
    page.getByRole("link", { name: "View explanation" }),
  ).toBeVisible();
  await page.getByRole("link", { name: "Suggestions", exact: true }).click();
  await page
    .getByLabel("Proposed scheme name")
    .fill("Browser suggested synthetic scheme");
  await page
    .getByLabel("Description", { exact: true })
    .fill("Please review this synthetic browser test suggestion.");
  await page
    .getByRole("button", { name: "Submit suggestion", exact: true })
    .click();
  await expect(
    page.getByRole("status").filter({ hasText: "Submitted" }),
  ).toBeVisible();
  await expect(page.getByText("Status: PENDING")).toBeVisible();
  await page.getByRole("button", { name: "Sign out", exact: true }).click();
  await expect(
    page.getByRole("heading", { name: "Sign in", exact: true }),
  ).toBeVisible();
  expect(
    (await context.cookies()).find((c) => c.name === "SESSION"),
  ).toBeUndefined();
  expect(errors).toEqual([]);
});
test("admin metadata, questions, versioned draft, publication, suggestion review and audit", async ({
  page,
}) => {
  const errors: string[] = [];
  page.on("pageerror", (e) => errors.push(e.message));
  page.on("console", m=>{if(m.type()==="error")errors.push(m.text());});
  await page.goto("/");
  await page.getByLabel("Email", { exact: true }).fill(env.ADMIN_EMAIL);
  await page.getByLabel("Password", { exact: true }).fill(env.ADMIN_PASSWORD);
  await page.getByRole("button", { name: "Sign in", exact: true }).click();
  await page.getByRole("link", { name: "Administration", exact: true }).click();
  await expect(
    page.getByRole("heading", { name: "Overview", exact: true }),
  ).toBeVisible();
  await page.getByRole("button", { name: "questions", exact: true }).click();
  await page
    .getByLabel("Question label")
    .fill("Browser question " + Date.now());
  await page.getByLabel("Answer type").selectOption("BOOLEAN");
  await page
    .getByRole("button", { name: "Save question", exact: true })
    .click();
  await expect(
    page.getByRole("status").filter({ hasText: "Question saved" }),
  ).toBeVisible();
  await page.getByRole("button", { name: "schemes", exact: true }).click();
  const name = "Synthetic Browser Demo " + Date.now();
  await page.getByLabel("Name", { exact: true }).fill(name);
  await page
    .getByLabel("Category", { exact: true })
    .selectOption({ label: "Synthetic demonstration" });
  await page
    .getByLabel("description", { exact: true })
    .fill("Fictional synthetic scheme created during browser validation.");
  await page
    .getByLabel("benefits", { exact: true })
    .fill("Demo only. No entitlement.");
  await page
    .getByLabel("guidance", { exact: true })
    .fill("No real application.");
  await page
    .getByRole("button", { name: "Create draft scheme", exact: true })
    .click();
  await expect(
    page.getByRole("status").filter({ hasText: "Metadata saved" }),
  ).toBeVisible();
  await page
    .getByRole("button", { name: "Validate and save draft", exact: true })
    .click();
  await expect(
    page.getByText("Rule validated and saved as draft."),
  ).toBeVisible();
  await page
    .getByRole("button", { name: "Publish saved draft", exact: true })
    .click();
  await expect(
    page.getByText("Published atomically. This version is immutable."),
  ).toBeVisible();
  await page.getByRole("button", { name: "suggestions", exact: true }).click();
  await page
    .getByRole("button", { name: "Review details", exact: true })
    .first()
    .click();
  await page.getByLabel("Review status").selectOption("ACCEPTED");
  await page.getByLabel("Review notes").fill("Accepted for draft review only.");
  await page.getByRole("button", { name: "Save review", exact: true }).click();
  await expect(page.getByText("Review saved.", { exact: true })).toBeVisible();
  await expect(
    page.getByRole("heading", {
      name: "Convert accepted suggestion to a draft",
    }),
  ).toBeVisible();
  const categories = await (
    await page.request.get("/api/v1/categories")
  ).json();
  await page
    .getByLabel("Draft JSON")
    .fill(
      JSON.stringify({
        name: "Synthetic Converted Browser Draft " + Date.now(),
        categoryId: categories[0].id,
        description: "Fictional converted draft",
        benefits: "No real benefit",
        guidance: "No application",
        synthetic: true,
      }),
    );
  await page
    .getByRole("button", { name: "Create draft from suggestion", exact: true })
    .click();
  await expect(
    page.getByText("Draft created. Review it in Schemes before publication.", {
      exact: true,
    }),
  ).toBeVisible();
  await page.getByRole("button", { name: "audit logs", exact: true }).click();
  await expect(page.locator("tbody")).toContainText("RULE_PUBLISHED");
  expect(errors).toEqual([]);
});
for (const width of [360, 768, 1440])
  for (const theme of ["light", "dark"])
    test(`responsive keyboard ${width}px ${theme}`, async ({ page }) => {
      await page.setViewportSize({ width, height: 900 });
      await page.addInitScript((t) => localStorage.setItem("theme", t), theme);
      await page.goto("/");
      await expect(
        page.getByRole("heading", { name: "Sign in", exact: true }),
      ).toBeVisible();
      await expect(page.locator("html")).toHaveAttribute("data-theme", theme);
      expect(
        await page.evaluate(
          () => document.documentElement.scrollWidth <= innerWidth,
        ),
      ).toBe(true);
      await page.keyboard.press("Tab");
      await expect(
        page.getByRole("link", { name: "Skip to content" }),
      ).toBeFocused();
      await page.keyboard.press("Enter");
      await expect(page.locator("main")).toBeFocused();
      await expect(
        page.getByRole("heading", { name: "Sign in", exact: true }),
      ).toBeVisible();
      await page.screenshot({
        path: `test-results/auth-${width}-${theme}.png`,
        fullPage: true,
      });
      await page
        .getByRole("button", { name: "Create a citizen account" })
        .click();
      await page.getByLabel("Full name").fill("Responsive Citizen");
      await page
        .getByLabel("Email", { exact: true })
        .fill(`responsive-${width}-${theme}-${Date.now()}@example.test`);
      await page
        .getByLabel("Password", { exact: true })
        .fill("responsivepass123");
      await page
        .getByRole("button", { name: "Create account", exact: true })
        .click();
      await expect(
        page.getByRole("heading", { name: "Welcome, Responsive Citizen" }),
      ).toBeVisible();
      for (const [label, title] of [
        ["My profile", "My profile"],
        ["Browse schemes", "Browse schemes"],
        ["Check history", "Eligibility history"],
        ["Suggestions", "Suggestions"],
      ]) {
        const menu = page.getByRole("button", { name: "Open navigation" });
        if (await menu.isVisible()) await menu.click();
        await page.getByRole("navigation", { name: "Main navigation" }).getByRole("link", { name: label, exact: true }).click();
        await expect(
          page.getByRole("heading", { name: title, exact: true }),
        ).toBeVisible();
        expect(
          await page.evaluate(
            () => document.documentElement.scrollWidth <= innerWidth,
          ),
        ).toBe(true);
        await page.screenshot({
          path: `test-results/${label.replaceAll(" ", "-")}-${width}-${theme}.png`,
          fullPage: true,
        });
      }
    });
