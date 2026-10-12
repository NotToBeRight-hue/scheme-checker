import React, { useState, useEffect, useId } from "react";

import { createRoot } from "react-dom/client";

import { api, bootstrapCsrf, ApiError, type Resource as R } from "./api";
import { ExactJsonBody, exactRuleBody } from "./exact-json";
import { displayText, pdfDemoSchemes, matchingOverview } from "./ui-model";

import s from "./App.module.css";

import "./theme.css";

const disclaimer =
  "Preliminary screening only. This prototype is not affiliated with the government and does not confirm entitlement or submit applications.";

const states = [
  "AN",
  "AP",
  "AR",
  "AS",
  "BR",
  "CH",
  "CG",
  "DN",
  "DL",
  "GA",
  "GJ",
  "HR",
  "HP",
  "JK",
  "JH",
  "KA",
  "KL",
  "LA",
  "LD",
  "MP",
  "MH",
  "MN",
  "ML",
  "MZ",
  "NL",
  "OD",
  "PY",
  "PB",
  "RJ",
  "SK",
  "TN",
  "TS",
  "TR",
  "UP",
  "UK",
  "WB",
];

const err = (e: unknown) =>
  e instanceof ApiError
    ? e.message +
      (e.status === 409
        ? " Reload this screen and review the latest values before submitting again."
        : e.code === "INPUT_REQUIRED"
          ? " Missing: " + e.details.missing?.join(", ")
          : "")
    : e instanceof Error
      ? e.message
      : "Request failed";

function useLoad(path: string | null) {
  const [data, setData] = useState<any>(null),
    [error, setError] = useState(""),
    [v, setV] = useState(0);
  useEffect(() => {
    let active = true;
    setData(null);
    setError("");
    if (path === null) return;
    api(path)
      .then((x) => {
        if (active) setData(x);
      })
      .catch((e) => {
        if (active) setError(err(e));
      });
    return () => {
      active = false;
    };
  }, [path, v]);
  return { data, error, reload: () => setV((x) => x + 1) };
}

function Load({
  data,
  error,
  children,
}: {
  data: any;
  error: string;
  children: React.ReactNode;
}) {
  return error ? (
    <p role="alert">{error}</p>
  ) : data === null ? (
    <p role="status">Loading...</p>
  ) : (
    <>{children}</>
  );
}

function Field({
  label,
  value,
  set,
  type = "text",
  options,
  required = false,
}: {
  label: string;
  value: any;
  set: (x: any) => void;
  type?: string;
  options?: [string, string][];
  required?: boolean;
}) {
  const id = useId();
  return (
    <div className={s.field}>
      <label htmlFor={id}>{label}</label>
      {options ? (
        <select
          id={id}
          value={value ?? ""}
          onChange={(e) => set(e.target.value)}
          required={required}
        >
          {options.map(([v, l]) => (
            <option key={v} value={v}>
              {l}
            </option>
          ))}
        </select>
      ) : type === "textarea" ? (
        <textarea
          id={id}
          value={value ?? ""}
          onChange={(e) => set(e.target.value)}
          required={required}
          maxLength={10000}
        />
      ) : (
        <input
          id={id}
          type={type}
          value={value ?? ""}
          onChange={(e) => set(e.target.value)}
          required={required}
          step={type === "number" ? "0.01" : undefined}
          maxLength={type === "password" ? 128 : 255}
        />
      )}
    </div>
  );
}

function Pager({
  page,
  total,
  size = 10,
  set,
}: {
  page: number;
  total: number;
  size?: number;
  set: (x: number) => void;
}) {
  return (
    <div className={s.pager}>
      <button disabled={!page} onClick={() => set(page - 1)}>
        Previous
      </button>
      <span>
        Page {page + 1} · {total} records
      </span>
      <button
        disabled={(page + 1) * size >= total}
        onClick={() => set(page + 1)}
      >
        Next
      </button>
    </div>
  );
}

function App() {
  const [navigationOpen, setNavigationOpen] = useState(false);
  const [lastMatching, setLastMatching] = useState<R | null>(null);
  const [pending, setPending] = useState(0);
  useEffect(() => {
    const busy = (e: Event) =>
      setPending((v) => Math.max(0, v + (e as CustomEvent).detail));
    window.addEventListener("api-busy", busy);
    return () => window.removeEventListener("api-busy", busy);
  }, []);
  const [user, setUser] = useState<R | null>(null),
    [loading, setLoading] = useState(true),
    [view, setView] = useState(location.hash.slice(1) || "dashboard"),
    [theme, setTheme] = useState(
      () => {
        try { const saved = localStorage.getItem("theme"); if (saved === "light" || saved === "dark") return saved; } catch { /* Use the system preference when storage is unavailable. */ }
        return matchMedia("(prefers-color-scheme: dark)").matches ? "dark" : "light";
      },
    ),
    [error, setError] = useState("");
  useEffect(() => {
    document.documentElement.dataset.theme = theme;
    try { localStorage.setItem("theme", theme); } catch { /* Theme remains usable when storage is unavailable. */ }
  }, [theme]);
  useEffect(() => {
    bootstrapCsrf()
      .then((signed) => (signed ? api("/auth/me") : null))
      .then(setUser)
      .catch(() => setUser(null))
      .finally(() => setLoading(false));
    const hash = () => {setView(location.hash.slice(1) || "dashboard");setNavigationOpen(false);window.scrollTo(0, 0);document.getElementById("content")?.focus({preventScroll: true});},
      expired = () => {setUser(null);setError("Your session expired. Sign in again.");};
    window.addEventListener("hashchange", hash);
    window.addEventListener("session-expired", expired);
    return () => {
      window.removeEventListener("hashchange", hash);
      window.removeEventListener("session-expired", expired);
    };
  }, []);
  useEffect(() => { setLastMatching(null); setNavigationOpen(false); }, [user?.id]);
  useEffect(() => {
    const close = (event: KeyboardEvent) => {
      if (navigationOpen && event.key === "Escape") { setNavigationOpen(false); document.getElementById("navigation-toggle")?.focus(); }
    };
    document.addEventListener("keydown", close);
    return () => document.removeEventListener("keydown", close);
  }, [navigationOpen]);
  return (
    <div className={`${s.shell} ${!user ? s.signedOut : ""}`}>
      <a
        className={s.skip}
        href="#content"
        onClick={(e) => {
          e.preventDefault();
          document.getElementById("content")?.focus();
        }}
      >
        Skip to content
      </a>
      <header className={s.topbar}>
        <a className={s.brand} href="#dashboard" aria-label="Government Scheme Selector home">
          <span className={s.logoFrame}><img className={s.lightAsset} src="/assets/logo_light.png" alt="Government Scheme Selector" /><img className={s.darkAsset} src="/assets/logo_dark.png" alt="Government Scheme Selector" /></span>
        </a>
        <div className={s.workspaceLabel}>Eligibility assistance prototype</div>
        {user && <a className={s.userIdentity} href="#profile" aria-label={"Open profile for " + user.fullName}><span className={s.avatar} aria-hidden="true">{user.fullName.split(/\s+/).map((part: string) => part[0]).slice(0, 2).join("")}</span><span>{user.fullName}<small>Your profile</small></span></a>}
        <button className={s.themeButton} onClick={() => setTheme(theme === "dark" ? "light" : "dark")}>
          {theme === "dark" ? "Light theme" : "Dark theme"}
        </button>
        {user && (
          <button
            onClick={async () => {
              try {
                await api("/auth/logout", "POST");
                setUser(null);
                setLastMatching(null);
              } catch (e) {
                setError(err(e));
              }
            }}
          >
            Sign out
          </button>
        )}
        {user && <button id="navigation-toggle" className={s.menuButton} aria-label={navigationOpen ? "Close navigation" : "Open navigation"} aria-controls="citizen-navigation" aria-expanded={navigationOpen} onClick={() => setNavigationOpen(!navigationOpen)}>Menu</button>}
      </header>
      {user && (
        <aside className={`${s.sidebar} ${navigationOpen ? s.navigationOpen : ""}`}><div className={s.sidebarHeading}>YOUR WORKSPACE</div><nav id="citizen-navigation" aria-label="Main navigation">
          {[
            ["dashboard", "Dashboard"],
            ["profile", "My profile"],
            ["schemes", "Browse schemes"],
            ["matching", "Match demo schemes"],
            ["schemes", "Individual eligibility"],
            ["history", "Check history"],
            ["suggestions", "Suggestions"],
            ...(user.role === "ADMIN" ? [["admin", "Administration"]] : []),
          ].map(([v, l]) => (
            <a
              key={l}
              href={"#" + v}
              aria-current={(view === v || (v === "schemes" && view.startsWith("scheme/")) || (v === "history" && view.startsWith("evaluation/"))) && l !== "Individual eligibility" ? "page" : undefined}
            >
              {l}
            </a>
          ))}
        </nav><p className={s.sidebarNote}>Fictional demonstration<br />Preliminary screening only</p></aside>
      )}
      <main id="content" className={s.main} tabIndex={-1} aria-busy={pending > 0}>
        {error && <p role="alert">{error}</p>}
        {pending > 0 && <p role="status">Saving changes...</p>}
        {loading ? (
          <p role="status">Connecting to your session...</p>
        ) : !user ? (
          <Auth login={u=>{setUser(u);setError('');location.hash='dashboard';setView('dashboard');}} />
        ) : view === "dashboard" ? (
          <Dashboard user={user} preview={lastMatching} />
        ) : view === "profile" ? (
          <Profile />
        ) : view === "schemes" ? (
          <Catalog />
        ) : view === "matching" ? (
          <Matching onPreview={setLastMatching} />
        ) : view.startsWith("scheme/") ? (
          <Detail key={view} id={view.split("/")[1]} />
        ) : view === "history" ? (
          <History />
        ) : view.startsWith("evaluation/") ? (
          <Evaluation id={view.split("/")[1]} />
        ) : view === "suggestions" ? (
          <Suggestions />
        ) : view === "admin" && user.role === "ADMIN" ? (
          <Admin />
        ) : (
          <p>
            Screen not found. <a href="#dashboard">Return to dashboard</a>
          </p>
        )}
      </main>
      <footer className={s.footer}>{disclaimer}</footer>
    </div>
  );
}

export function Auth({ login }: { login: (u: R) => void }) {
  const [register, setRegister] = useState(false),
    [f, setF] = useState<R>({ fullName: "", email: "", password: "" }),
    [error, setError] = useState(""),
    [busy, setBusy] = useState(false);
  const change = (k: string, v: any) => setF({ ...f, [k]: v });
  return (
    <section className={s.auth}>
      <h1>Find schemes and understand eligibility</h1>
      <p>
        Browse published schemes, check your details against their rules and see
        the reasons for each result.
      </p>
      <p className={s.notice}>
        The local demonstration catalog contains fictional synthetic schemes.
        These are not real government programmes.
      </p>
      <h2>{register ? "Create a citizen account" : "Sign in"}</h2>
      <form
        onSubmit={async (e) => {
          e.preventDefault();
          setBusy(true);
          setError("");
          try {
            login(
              await api(register ? "/auth/register" : "/auth/login", "POST", f),
            );
          } catch (e) {
            setError(err(e));
          } finally {
            setBusy(false);
          }
        }}
      >
        {register && (
          <Field
            label="Full name"
            value={f.fullName}
            set={(v) => change("fullName", v)}
            required
          />
        )}
        <Field
          label="Email"
          type="email"
          value={f.email}
          set={(v) => change("email", v)}
          required
        />
        <Field
          label="Password"
          type="password"
          value={f.password}
          set={(v) => change("password", v)}
          required
        />
        <p>8 to 128 characters. Sessions last 15 minutes.</p>
        {error && <p role="alert">{error}</p>}
        <button disabled={busy}>
          {busy ? "Please wait..." : register ? "Create account" : "Sign in"}
        </button>
      </form>
      <button
        className={s.secondary}
        onClick={() => {
          setRegister(!register);
          setError("");
        }}
      >
        {register ? "Already registered? Sign in" : "Create a citizen account"}
      </button>
    </section>
  );
}

function Checks({ rows }: { rows: R[] }) {
  return !rows.length ? (
    <p>
      No checks yet. <a href="#schemes">Choose a scheme</a>.
    </p>
  ) : (
    <div className={s.grid}>
      {rows.map((e) => (
        <section key={e.id}>
          <h3>{e.snapshot?.schemeName || "Scheme " + e.schemeId}</h3>
          <p>{e.verdict.replaceAll("_", " ")}</p>
          <p>
            {e.evaluationDate} ·{" "}
            {e.stale
              ? "Stale: " + e.staleReasons.join(", ")
              : "Current snapshot"}
          </p>
          <a href={"#evaluation/" + e.id}>View explanation</a>
        </section>
      ))}
    </div>
  );
}

function SchemeCard({ scheme, categories, heading = "h3" }: { scheme: R; categories: R[]; heading?: "h2" | "h3" }) {
  const Heading = heading;
  return <section className={s.schemeCard}>
    <div className={s.cardHeading}><span className={s.category}>{categories.find(category => category.id === scheme.categoryId)?.name || "Category unavailable"}</span><Badge demo={scheme.synthetic} /></div>
    <Heading>{displayText(scheme.name)}</Heading>
    <p className={s.benefit}>{displayText(scheme.benefits)}</p>
    <p className={s.schemeDescription}>{displayText(scheme.description)}</p>
    <p className={s.scope}>{scheme.stateCode || "National scope"}</p>
    <a className={s.cardLink} href={"#scheme/" + scheme.id}>View scheme</a>
  </section>;
}

export function Dashboard({ user, preview = null }: { user: R; preview?: R | null }) {
  const dashboard = useLoad("/dashboard"), profile = useLoad("/profile"), catalog = useLoad("/schemes?size=100"), categories = useLoad("/categories");
  const overview = profile.data && catalog.data ? matchingOverview(preview, profile.data.revision, catalog.data.items) : null;
  const featured = pdfDemoSchemes(catalog.data?.items || []);
  return <>
    <p className={s.eyebrow}>CITIZEN DASHBOARD</p>
    <section className={s.hero}>
      <div className={s.heroContent}><h1>Welcome, {user.fullName}</h1><p>Review your answers and explore the fictional demonstration schemes.</p>
        <div className={s.actions}><a className={s.primaryLink} href="#matching">Start Matching</a><a className={s.secondaryLink} href="#schemes">Browse All Schemes</a></div>
        <p className={s.heroDisclaimer}>FICTIONAL / DEMO ONLY. No government affiliation or entitlement.</p>
      </div>
      <div className={s.heroArtwork}><img className={s.lightAsset} src="/assets/hero_light.png" alt="Government building illustration in daylight" /><img className={s.darkAsset} src="/assets/hero_dark.png" alt="Government building illustration at night" /></div>
    </section>
    <div className={s.dashboardColumns}><div className={s.dashboardContent}>
      <section aria-label="Eligibility overview"><div className={s.sectionHeading}><h2>Eligibility overview</h2><a href="#matching">Open matching</a></div>
        {profile.error || catalog.error ? <p role="alert">{profile.error || catalog.error}</p> : !profile.data || !catalog.data ? <p role="status">Loading overview...</p> : overview ? <div className={s.overviewGrid}>
          {[["Eligible schemes", overview.eligible, "eligible"], ["Not eligible schemes", overview.notEligible, "negative"], ["Need more information", overview.needsInformation, "unknown"], ["Total schemes evaluated", overview.total, "total"]].map(([label, count, style]) => <div className={s.overviewTile} data-status={style} key={label}><span>{label}</span><strong>{count}</strong></div>)}
        </div> : <div className={s.overviewEmpty}><p>{preview ? "Your answers or published rules have changed. Run matching again for current results." : "No matching preview in this session yet."}</p><p>Start matching to see your overview. Previews do not add eligibility history.</p><a href="#matching">Start matching now</a></div>}
      </section>
      <div className={s.sectionHeading}><h2>Featured demo schemes</h2><a href="#schemes">Browse all</a></div>
      <p className={s.muted}>Fictional scheme benefits are for demonstration only.</p>
      <Load {...catalog}><Load {...categories}>{!featured.length ? <section><p>No published PDF demo schemes are available.</p></section> : <div className={s.featuredGrid}>{featured.map(scheme => <SchemeCard key={scheme.id} scheme={scheme} categories={categories.data || []} />)}</div>}</Load></Load>
    </div><aside className={s.rightPanels} aria-label="Profile, activity and help">
      <section><h2>Your profile</h2><p className={s.profileName}>{user.fullName}</p><p className={s.muted}>{user.email}</p>
        <Load {...dashboard}><p>{dashboard.data?.profileCompleteness}% complete</p><progress max={100} value={dashboard.data?.profileCompleteness} /></Load>
        <Load {...profile}><dl className={s.profileSummary}>{[["State", profile.data?.profile.stateCode], ["Education", profile.data?.profile.educationLevel], ["Occupation", profile.data?.profile.occupation], ["Annual income", profile.data?.profile.annualIncome == null ? null : "INR " + profile.data.profile.annualIncome]].map(([label, value]) => <div key={label}><dt>{label}</dt><dd>{value || "Not provided"}</dd></div>)}</dl></Load><a href="#profile">Complete or update profile</a>
      </section>
      <section><div className={s.sectionHeading}><h2>Recent activity</h2><a href="#history">View history</a></div><Load {...dashboard}>
        {!dashboard.data?.recentChecks.length ? <p className={s.muted}>No individual checks yet. Matching previews are not recorded here.</p> : <ol className={s.recentActivity}>{dashboard.data.recentChecks.map((entry: R) => <li key={entry.id}><a href={"#evaluation/" + entry.id}>{displayText(entry.snapshot?.schemeName || "Scheme " + entry.schemeId)}</a><span>{entry.verdict.replaceAll("_", " ")} | {entry.evaluationDate}</span>{entry.stale && <span>Stale: {entry.staleReasons.join(", ")}</span>}</li>)}</ol>}
        <p>{dashboard.data?.evaluationCount} completed checks</p><p>{dashboard.data?.pendingSuggestions} suggestions awaiting review</p>
      </Load></section>
      <section className={s.helpPanel}><h2>Need help?</h2><p>Leave uncertain answers as Unknown. Review each explanation before choosing an individual check.</p><a href="#matching">Open shared questionnaire</a><a href="#schemes">Choose a scheme to check</a><a href="#suggestions">Suggest a scheme or correction</a></section>
    </aside></div>
  </>;
}

function Profile() {
  const l = useLoad("/profile"),
    [f, setF] = useState<R>({}),
    [error, setError] = useState(""),
    [message, setMessage] = useState(""),
    [busy, setBusy] = useState(false);
  useEffect(() => {
    if (l.data) setF(l.data.profile);
  }, [l.data]);
  const change = (k: string, v: any) => setF({ ...f, [k]: v });
  return (
    <>
      <h1>My profile</h1>
      <p>Leave unknown values blank. Annual income is in Indian rupees.</p>
      <button onClick={l.reload}>Reload profile</button>
      <Load {...l}>
        <form
          className={s.form}
          onSubmit={async (e) => {
            e.preventDefault();
            setBusy(true);
            setError("");
            try {
              await api("/profile", "PUT", {
                ...f,
                expectedRevision: l.data.revision,
              });
              setMessage("Profile saved. Earlier checks may now be stale.");
              l.reload();
            } catch (e) {
              setError(err(e));
            } finally {
              setBusy(false);
            }
          }}
        >
          <Field
            label="Date of birth"
            type="date"
            value={f.dateOfBirth}
            set={(v) => change("dateOfBirth", v)}
          />
          <Field
            label="State or union territory"
            value={f.stateCode}
            set={(v) => change("stateCode", v)}
            options={[
              ["", "Unknown"],
              ...states.map((x) => [x, x] as [string, string]),
            ]}
          />
          <Field
            label="Annual income (INR)"
            type="number"
            value={f.annualIncome}
            set={(v) => change("annualIncome", v)}
          />
          <Field
            label="Education level"
            value={f.educationLevel}
            set={(v) => change("educationLevel", v)}
            options={[
              "",
              "PRIMARY",
              "SECONDARY",
              "HIGHER_SECONDARY",
              "GRADUATE",
              "POSTGRADUATE",
              "OTHER",
            ].map((x) => [x, x || "Unknown"])}
          />
          <Field
            label="Occupation"
            value={f.occupation}
            set={(v) => change("occupation", v)}
          />
          {error && <p role="alert">{error}</p>}
          {message && <p role="status">{message}</p>}
          <button disabled={busy}>{busy ? "Saving..." : "Save profile"}</button>
        </form>
      </Load>
    </>
  );
}

function Catalog() {
  const cats = useLoad("/categories"),
    [search, setSearch] = useState(""),
    [state, setState] = useState(""),
    [category, setCategory] = useState(""),
    [page, setPage] = useState(0);
  const l = useLoad(
    "/schemes?" +
      new URLSearchParams({
        search,
        ...(state ? { stateCode: state } : {}),
        ...(category ? { categoryId: category } : {}),
        page: String(page),
        size: "6",
      }),
  );
  return (
    <>
      <h1>Browse schemes</h1>
      <p>Only schemes with a published rule version appear here.</p>
      <div className={s.filters}>
        <Field
          label="Search schemes"
          value={search}
          set={(v) => {
            setSearch(v);
            setPage(0);
          }}
        />
        <Field
          label="State"
          value={state}
          set={(v) => {
            setState(v);
            setPage(0);
          }}
          options={[
            ["", "All states"],
            ...states.map((x) => [x, x] as [string, string]),
          ]}
        />
        <Field
          label="Category"
          value={category}
          set={(v) => {
            setCategory(v);
            setPage(0);
          }}
          options={[
            ["", "All categories"],
            ...(cats.data || []).map((c: R) => [c.id, c.name]),
          ]}
        />
      </div>
      <Load {...l}>
        {!l.data?.items.length ? (
          <p>No published schemes match. Try a broader search.</p>
        ) : (
          <div className={s.grid}>
            {l.data?.items.map((x: R) => (
              <SchemeCard key={x.id} scheme={x} categories={cats.data || []} heading="h2" />
            ))}
          </div>
        )}
        <Pager page={page} total={l.data?.total || 0} size={6} set={setPage} />
      </Load>
    </>
  );
}

function Badge({ demo }: { demo: boolean }) {
  return (
    <span className={s.badge}>
      {demo ? "Fictional synthetic demo" : "Verified source scheme"}
    </span>
  );
}

function RuleView({ rule, labels = {} }: { rule: R; labels?: Record<string, string> }) {
  return !rule ? null : rule.type === "GROUP" ? (
    <div className={s.rule}>
      <strong>
        {rule.operator === "AND" ? "All conditions" : "At least one condition"}
      </strong>
      {rule.children.map((c: R, i: number) => (
          <RuleView key={i} rule={c} labels={labels} />
      ))}
    </div>
  ) : (
    <p>
      {labels[rule.field] || rule.field} {rule.operator} {JSON.stringify(rule.value)}
    </p>
  );
}

function QuestionInput({
  q,
  value,
  set,
}: {
  q: R;
  value: any;
  set: (v: any) => void;
}) {
  if (q.type === "MULTI_SELECT")
    return (
      <fieldset>
        <legend>{q.label}</legend>
        {q.options.map((o: string) => (
          <label key={o}>
            <input
              type="checkbox"
              checked={Array.isArray(value) && value.includes(o)}
              onChange={(e) =>
                set(
                  e.target.checked
                    ? [...(value || []), o]
                    : (value || []).filter((x: string) => x !== o),
                )
              }
            />
            {o}
          </label>
        ))}
      </fieldset>
    );
  return (
    <Field
      label={q.label}
      value={
        q.type === "BOOLEAN" ? (value == null ? "" : String(value)) : value
      }
      set={(v) =>
        set(q.type === "BOOLEAN" ? (v === "" ? null : v === "true") : v)
      }
      type={
        q.type === "NUMBER" ? "number" : q.type === "DATE" ? "date" : "text"
      }
      options={
        q.type === "BOOLEAN"
          ? [
              ["", "Unknown"],
              ["true", "Yes"],
              ["false", "No"],
            ]
          : q.type === "SELECT"
            ? [["", "Unknown"], ...q.options.map((o: string) => [o, o])]
            : undefined
      }
    />
  );
}

export function Matching({ onPreview }: { onPreview?: (result: R | null) => void } = {}) {
  const questionnaire = useLoad("/matching/questions");
  const [answers, setAnswers] = useState<R>({});
  const [revision, setRevision] = useState("");
  const [dirty, setDirty] = useState(false);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  const [result, setResult] = useState<R | null>(null);
  useEffect(() => {
    if (questionnaire.data) {
      setAnswers(Object.fromEntries(questionnaire.data.questions.map((q: R) => [q.id, q.value ?? null])));
      setRevision(questionnaire.data.revision);
      setDirty(false);
    }
  }, [questionnaire.data]);
  async function preview(savedRevision: string) {
    const response = await api("/matching/preview?expectedProfileRevision=" + encodeURIComponent(savedRevision));
    setResult(response); onPreview?.(response);
  }
  return <>
    <h1>Match demo schemes</h1>
    <p className={s.notice}>FICTIONAL / DEMO ONLY. These eight schemes and benefits are invented. No money, government approval or application is offered.</p>
    <p>Answer the shared questions once. Your saved answers are reused in individual scheme checks. Profile fields are not substituted for these answers. Matching previews do not save eligibility history.</p>
    <Load {...questionnaire}>
      {error && <p role="alert">{error}</p>}
      <form aria-label="Shared demo questionnaire" onSubmit={async e => {
        e.preventDefault(); setBusy(true); setError(""); setResult(null); onPreview?.(null);
        try {
          const saved = await api("/matching/answers", "PUT", {
            expectedProfileRevision: revision,
            answers: questionnaire.data.questions.map((q: R) => ({ questionId: q.id, value: answers[q.id] === "" ? null : answers[q.id] ?? null })),
          });
          setRevision(saved.revision); setDirty(false); await preview(saved.revision);
        } catch (e) { setError(err(e)); } finally { setBusy(false); }
      }}>
        <fieldset disabled={busy}><legend>Shared eligibility answers</legend>
        {questionnaire.data?.questions.map((q: R) => <QuestionInput key={q.id} q={q} value={answers[q.id]} set={value => {
          setAnswers(current => ({ ...current, [q.id]: value })); setDirty(true); setResult(null); onPreview?.(null); setError("");
        }} />)}
        </fieldset>
        <button disabled={busy}>Save answers and match</button>
      </form>
      <button disabled={busy || dirty} onClick={async () => {
        setBusy(true); setError(""); setResult(null); onPreview?.(null);
        try { await preview(revision); } catch (e) { setError(err(e)); } finally { setBusy(false); }
      }}>Preview saved answers</button>
      <button disabled={busy} onClick={() => { setResult(null); onPreview?.(null); setError(""); questionnaire.reload(); }}>Reload shared answers</button>
      {dirty && <p>Save your changed answers before previewing.</p>}
      {busy && <p role="status">Checking the current published rules...</p>}
    </Load>
    {result && <section aria-label="Matching results">
      <h2>Matching results</h2>
      <p>{result.disclaimer}</p>
      <div className={s.grid}>{result.results.map((row: R) => <section key={row.schemeId} aria-label={row.schemeName} data-classification={row.classification}>
        <h3>{row.schemeName}</h3><Badge demo={row.synthetic} />
        <p className={s.benefit}>{displayText(row.benefits)}</p><p><strong>{row.classification.replaceAll("_", " ")}</strong></p>
        {row.classification === "NEEDS_INFORMATION" && <><p>Missing questions:</p><ul>{row.missingQuestions.map((q: R) => <li key={q.fieldKey}>{q.label}</li>)}</ul></>}
        {row.classification === "NOT_ELIGIBLE" && <><p>Conditions not met:</p><ul>{row.explanations.filter((e: R) => e.result === "FALSE").map((e: R) => <li key={e.nodeId}>{e.questionLabel}: expected {e.operator} {JSON.stringify(e.expectedValue)}; answered {JSON.stringify(e.actualValue)}.</li>)}</ul></>}
        <details><summary>Every evaluated condition</summary><ul>{row.explanations.map((e: R) => <li key={e.nodeId}>{e.questionLabel}: {e.operator} {JSON.stringify(e.expectedValue)}; answered {e.actualValue == null ? "Unknown" : JSON.stringify(e.actualValue)}; {e.result}.</li>)}</ul></details>
        <p>Published rule version {row.ruleVersionId}</p>
        <a href={"#scheme/" + row.schemeId}>View scheme and check individually</a>
      </section>)}</div>
    </section>}
  </>;
}

function Detail({ id }: { id: string }) {
  const l = useLoad("/schemes/" + id),
    q = useLoad("/schemes/" + id + "/questions"),
    p = useLoad("/profile"),
    [a, setA] = useState<R>({}),
    [error, setError] = useState(""),
    [message, setMessage] = useState(""),
    [busy, setBusy] = useState(false);
  useEffect(() => {
    if (q.data) {
      const values: R = {};
      q.data.forEach((x: R) => (values[x.id] = x.value ?? null));
      setA(values);
    }
  }, [q.data]);
  return (
    <>
      <a href="#schemes">Back to catalog</a>
      <Load {...l}>
        <h1>{l.data?.name}</h1>
        <Badge demo={l.data?.synthetic} />
        <p>{displayText(l.data?.description)}</p>
        <div className={s.grid}>
          <section>
            <h2>Benefits</h2>
            <p className={s.benefit}>{displayText(l.data?.benefits)}</p>
          </section>
          <section>
            <h2>Application and document guidance</h2>
            <p>{displayText(l.data?.guidance)}</p>
            {l.data?.sourceUrl && (
              <a
                href={l.data.sourceUrl}
                target="_blank"
                rel="noopener noreferrer"
              >
                Official source
              </a>
            )}
            <p>{l.data?.verifiedDate && "Verified " + l.data.verifiedDate}</p>
          </section>
        </div>
        <section>
          <h2>Eligibility criteria</h2>
          <RuleView rule={l.data?.rule} labels={Object.fromEntries((q.data || []).map((question: R) => ["question:" + question.id, question.label]))} />
        </section>
        <section>
          <h2>Questions and eligibility check</h2>
          <p>
            Save answers before checking. Shared answers update every linked
            scheme.
          </p>
          <Load data={q.data && p.data} error={q.error || p.error}>
            <form
              onSubmit={async (e) => {
                e.preventDefault();
                setBusy(true);
                setError("");
                try {
                  await api("/schemes/" + id + "/answers", "PUT", {
                    expectedProfileRevision: p.data.revision,
                    answers: q.data.map((x: R) => ({
                      questionId: x.id,
                      value: a[x.id] === "" ? null : a[x.id],
                    })),
                  });
                  setMessage("Answers saved.");
                  p.reload();
                  q.reload();
                } catch (e) {
                  setError(err(e));
                } finally {
                  setBusy(false);
                }
              }}
            >
              {!q.data?.length && <p>No additional questions.</p>}
              {q.data?.map((x: R) => (
                <QuestionInput
                  key={x.id}
                  q={x}
                  value={a[x.id]}
                  set={(v) => setA({ ...a, [x.id]: v })}
                />
              ))}
              <button disabled={busy}>Save answers</button>
            </form>
            <button
              disabled={busy}
              onClick={async () => {
                setBusy(true);
                setError("");
                try {
                  const r = await api("/eligibility/evaluate", "POST", {
                    schemeId: id,
                    expectedProfileRevision: p.data.revision,
                  });
                  location.hash = "evaluation/" + r.evaluationId;
                } catch (e) {
                  setError(err(e));
                } finally {
                  setBusy(false);
                }
              }}
            >
              Check eligibility
            </button>
            <button
              onClick={() => {
                p.reload();
                q.reload();
              }}
            >
              Reload latest inputs
            </button>
          </Load>
          {error && (
            <p role="alert">
              {error} <a href="#profile">Edit profile</a>
            </p>
          )}
          {message && <p role="status">{message}</p>}
        </section>
      </Load>
    </>
  );
}

function History() {
  const [page, setPage] = useState(0),
    l = useLoad("/eligibility/history?page=" + page + "&size=10");
  return (
    <>
      <h1>Eligibility history</h1>
      <p>
        Each immutable snapshot preserves the details and rule version used in
        its check.
      </p>
      <Load {...l}>
        <Checks rows={l.data?.items || []} />
        <Pager page={page} total={l.data?.total || 0} set={setPage} />
      </Load>
    </>
  );
}

function Evaluation({ id }: { id: string }) {
  const l = useLoad("/eligibility/history/" + id);
  const questions = useLoad(l.data?.schemeId ? "/schemes/" + l.data.schemeId + "/questions" : null);
  const labels = Object.fromEntries((questions.data || []).map((question: R) => ["question:" + question.id, question.label]));
  return (
    <>
      <a href="#history">Back to history</a>
      <h1>Eligibility explanation</h1>
      <Load {...l}>
        <section>
          <h2>{l.data?.snapshot?.schemeName}</h2>
          <p className={s.verdict} data-verdict={l.data?.verdict}>{l.data?.verdict.replaceAll("_", " ")}</p>
          <p>
            Rule version {l.data?.ruleVersionId} · Profile revision{" "}
            {l.data?.profileRevision} · {l.data?.evaluationDate} (Asia/Kolkata)
          </p>
          {l.data?.stale && (
            <p className={s.notice}>
              Stale: {l.data.staleReasons.join(", ")}. Run a new check for
              current details.
            </p>
          )}
          <p>{disclaimer}</p>
          <div className={s.tableWrap}>
            <table>
              <caption>Every evaluated condition</caption>
              <thead>
                <tr>
                  {["Field", "Operator", "Expected", "Actual", "Result"].map(
                    (x) => (
                      <th key={x}>{x}</th>
                    ),
                  )}
                </tr>
              </thead>
              <tbody>
                {l.data?.explanations.map((x: R) => (
                  <tr key={x.nodeId}>
                    <td>{labels[x.fieldKey] || x.fieldKey}</td>
                    <td>{x.operator}</td>
                    <td>{JSON.stringify(x.expectedValue)}</td>
                    <td>
                      {x.actualValue == null
                        ? "Unknown"
                        : JSON.stringify(x.actualValue)}
                    </td>
                    <td>{x.result}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          <details>
            <summary>Immutable facts and rule snapshot</summary>
            <pre>{JSON.stringify(l.data?.snapshot, null, 2)}</pre>
          </details>
          <a href={"#scheme/" + l.data?.schemeId}>Run a new check</a>
        </section>
      </Load>
    </>
  );
}

function Suggestions() {
  const [page, setPage] = useState(0),
    l = useLoad("/suggestions/mine?page=" + page + "&size=10"),
    schemes = useLoad("/schemes?size=100"),
    [f, setF] = useState<R>({
      type: "SCHEME",
      schemeName: "",
      schemeId: "",
      description: "",
      proposedCriterion: "",
      sourceUrl: "",
    }),
    [error, setError] = useState(""),
    [message, setMessage] = useState(""),
    [busy, setBusy] = useState(false);
  const change = (k: string, v: any) => setF({ ...f, [k]: v });
  return (
    <>
      <h1>Suggestions</h1>
      <p>
        Acceptance does not automatically publish a scheme or change a rule.
      </p>
      <section>
        <h2>Submit a suggestion</h2>
        <form
          className={s.form}
          onSubmit={async (e) => {
            e.preventDefault();
            setBusy(true);
            setError("");
            try {
              await api("/suggestions", "POST", {
                ...f,
                schemeId: f.schemeId || null,
              });
              setMessage("Submitted for review.");
              change("description", "");
              l.reload();
            } catch (e) {
              setError(err(e));
            } finally {
              setBusy(false);
            }
          }}
        >
          <Field
            label="Suggestion type"
            value={f.type}
            set={(v) => change("type", v)}
            options={[
              ["SCHEME", "New scheme"],
              ["CRITERION", "Criterion correction"],
            ]}
          />
          {f.type === "SCHEME" ? (
            <Field
              label="Proposed scheme name"
              value={f.schemeName}
              set={(v) => change("schemeName", v)}
              required
            />
          ) : (
            <Field
              label="Scheme"
              value={f.schemeId}
              set={(v) => change("schemeId", v)}
              options={[
                ["", "Select scheme"],
                ...(schemes.data?.items || []).map((x: R) => [x.id, x.name]),
              ]}
              required
            />
          )}
          <Field
            label="Description"
            type="textarea"
            value={f.description}
            set={(v) => change("description", v)}
            required
          />
          {f.type === "CRITERION" && (
            <Field
              label="Proposed criterion"
              type="textarea"
              value={f.proposedCriterion}
              set={(v) => change("proposedCriterion", v)}
            />
          )}
          <Field
            label="Source URL (HTTPS, optional)"
            type="url"
            value={f.sourceUrl}
            set={(v) => change("sourceUrl", v)}
          />
          {error && <p role="alert">{error}</p>}
          {message && <p role="status">{message}</p>}
          <button disabled={busy}>Submit suggestion</button>
        </form>
      </section>
      <h2>My submissions</h2>
      <Load {...l}>
        {!l.data?.items.length ? (
          <p>No suggestions submitted yet.</p>
        ) : (
          l.data.items.map((x: R) => (
            <section key={x.id}>
              <h3>
                {x.schemeName ||
                  "Criterion correction for scheme " + x.schemeId}
              </h3>
              <p>{x.description}</p>
              <p>Status: {x.status.replaceAll("_", " ")}</p>
              <p>{x.reviewNotes || "Awaiting review notes."}</p>
            </section>
          ))
        )}
        <Pager page={page} total={l.data?.total || 0} set={setPage} />
      </Load>
    </>
  );
}

const defaultRule = {
  schemaVersion: 1,
  type: "GROUP",
  operator: "AND",
  children: [
    {
      type: "CONDITION",
      nodeId: "age",
      field: "completedAgeYears",
      operator: "GTE",
      value: 18,
    },
  ],
};

function Admin() {
  const [tab, setTab] = useState("overview");
  return (
    <>
      <h1>Administration</h1>
      <div className={s.tabs}>
        {[
          "overview",
          "schemes",
          "categories",
          "questions",
          "suggestions",
          "audit-logs",
        ].map((t) => (
          <button key={t} aria-pressed={tab === t} onClick={() => setTab(t)}>
            {t.replaceAll("-", " ")}
          </button>
        ))}
      </div>
      {tab === "overview" ? (
        <AdminOverview />
      ) : tab === "schemes" ? (
        <AdminSchemes />
      ) : tab === "categories" ? (
        <AdminCategories />
      ) : tab === "questions" ? (
        <AdminQuestions />
      ) : tab === "suggestions" ? (
        <AdminReviews />
      ) : (
        <AdminAudit />
      )}
    </>
  );
}

function AdminOverview() {
  const l = useLoad("/admin/overview");
  return (
    <Load {...l}>
      <section>
        <h2>Overview</h2>
        <p>{l.data?.schemes} scheme records</p>
        <p>{l.data?.pendingSuggestions} pending suggestions</p>
        <p>
          Create metadata, link typed questions, validate a draft rule and
          publish its immutable version. Genuine schemes require official source
          verification and a source section for every criterion.
        </p>
      </section>
    </Load>
  );
}

function AdminCategories() {
  const l = useLoad("/categories"),
    [selected, setSelected] = useState<R | null>(null),
    [name, setName] = useState(""),
    [error, setError] = useState(""),
    [message, setMessage] = useState("");
  return (
    <section>
      <h2>Categories</h2>
      <Load {...l}>
        {l.data?.map((c: R) => (
          <button
            key={c.id}
            onClick={() => {
              setSelected(c);
              setName(c.name);
            }}
          >
            {c.name}
          </button>
        ))}
      </Load>
      <form
        onSubmit={async (e) => {
          e.preventDefault();
          try {
            await api(
              "/admin/categories" + (selected ? "/" + selected.id : ""),
              selected ? "PUT" : "POST",
              { name },
            );
            setName("");
            setSelected(null);
            l.reload();
            setError("");
            setMessage("Category saved.");
          } catch (e) {
            setError(err(e));
          }
        }}
      >
        <Field label="Category name" value={name} set={setName} required />
        <button>{selected ? "Update category" : "Create category"}</button>
        <button
          type="button"
          onClick={() => {
            setSelected(null);
            setName("");
          }}
        >
          New category
        </button>
        {error && <p role="alert">{error}</p>}
        {message && <p role="status">{message}</p>}
      </form>
    </section>
  );
}

const emptyScheme = {
  name: "",
  categoryId: "",
  stateCode: "",
  description: "",
  benefits: "",
  guidance: "",
  sourceUrl: "",
  verifiedDate: "",
  synthetic: true,
  reviewerApproved: false,
  sourceMapping: {},
  exceptions: "",
  questionIds: [],
};

function AdminSchemes() {
  const l = useLoad("/admin/schemes"),
    cats = useLoad("/categories"),
    questions = useLoad("/admin/questions"),
    [record, setRecord] = useState<R>({ ...emptyScheme }),
    [mapping, setMapping] = useState("{}"),
    [error, setError] = useState(""),
    [message, setMessage] = useState("");
  const change = (k: string, v: any) => setRecord({ ...record, [k]: v });
  async function select(x: R) {
    setRecord({ ...x, questionIds: [] });
    setMapping(JSON.stringify(x.sourceMapping || {}, null, 2));
    setError("");
    if (x.id && !x.publishedVersionId) {
      try {
        const linked = await api("/admin/schemes/" + x.id + "/questions");
        setRecord({ ...x, questionIds: linked.map((q: R) => q.id) });
      } catch (e) {
        setError(err(e));
      }
    }
  }
  return (
    <>
      <h2>Schemes and publication</h2>
      <button onClick={() => select({ ...emptyScheme })}>
        New draft scheme
      </button>
      <button onClick={l.reload}>Reload scheme list</button>
      <Load {...l}>
        <div className={s.grid}>
          {l.data?.map((x: R) => (
            <section key={x.id}>
              <h3>{x.name}</h3>
              <p>
                {x.publishedVersionId
                  ? "Published version " + x.publishedVersionId
                  : "Draft metadata"}
              </p>
              <button onClick={() => select(x)}>Manage</button>
            </section>
          ))}
        </div>
      </Load>
      <section>
        <h3>{record.id ? "Manage scheme " + record.id : "Create scheme"}</h3>
        <form
          className={s.form}
          onSubmit={async (e) => {
            e.preventDefault();
            try {
              const payload = {
                ...record,
                sourceMapping: JSON.parse(mapping),
                expectedRevision: record.revision,
              };
              const r = await api(
                "/admin/schemes" + (record.id ? "/" + record.id : ""),
                record.id ? "PUT" : "POST",
                payload,
              );
              await select(r);
              setMessage("Metadata saved.");
              l.reload();
              setError("");
            } catch (e) {
              setError(err(e));
            }
          }}
        >
          <fieldset disabled={!!record.publishedVersionId}>
            <legend>Metadata</legend>
            <Field
              label="Name"
              value={record.name}
              set={(v) => change("name", v)}
              required
            />
            <Field
              label="Category"
              value={record.categoryId}
              set={(v) => change("categoryId", v)}
              required
              options={[
                ["", "Select category"],
                ...(cats.data || []).map((x: R) => [x.id, x.name]),
              ]}
            />
            <Field
              label="Scope state"
              value={record.stateCode}
              set={(v) => change("stateCode", v)}
              options={[
                ["", "National"],
                ...states.map((x) => [x, x] as [string, string]),
              ]}
            />
            {["description", "benefits", "guidance", "exceptions"].map((k) => (
              <Field
                key={k}
                label={k}
                type="textarea"
                value={record[k]}
                set={(v) => change(k, v)}
                required={k !== "exceptions"}
              />
            ))}
            <label>
              <input
                type="checkbox"
                checked={record.synthetic}
                onChange={(e) => change("synthetic", e.target.checked)}
              />
              Fictional synthetic development scheme
            </label>
            <Field
              label="Official HTTPS source URL"
              type="url"
              value={record.sourceUrl}
              set={(v) => change("sourceUrl", v)}
            />
            <Field
              label="Source verified date"
              type="date"
              value={record.verifiedDate}
              set={(v) => change("verifiedDate", v)}
            />
            <label>
              <input
                type="checkbox"
                checked={record.reviewerApproved}
                onChange={(e) => change("reviewerApproved", e.target.checked)}
              />
              Reviewer approves verified source and criteria
            </label>
            <Field
              label="Source section per node ID (JSON object)"
              type="textarea"
              value={mapping}
              set={setMapping}
            />
            <fieldset>
              <legend>Linked questions</legend>
              {questions.data?.map((q: R) => (
                <label key={q.id}>
                  <input
                    type="checkbox"
                    checked={record.questionIds?.includes(q.id)}
                    onChange={(e) =>
                      change(
                        "questionIds",
                        e.target.checked
                          ? [...(record.questionIds || []), q.id]
                          : (record.questionIds || []).filter(
                              (x: string) => x !== q.id,
                            ),
                      )
                    }
                  />
                  {q.id}: {q.label}
                </label>
              ))}
            </fieldset>
            <button>
              {record.id ? "Save metadata" : "Create draft scheme"}
            </button>
          </fieldset>
        </form>
        {record.publishedVersionId && (
          <p>
            Published metadata is immutable. Create a new rule version below, or
            create a new draft scheme to revise metadata.
          </p>
        )}
        {error && <p role="alert">{error}</p>}
        {message && <p role="status">{message}</p>}
        {record.id && (
          <AdminRules
            key={record.id}
            scheme={record}
            published={() => {
              l.reload();
              select({ ...record, publishedVersionId: "published" });
            }}
          />
        )}
      </section>
    </>
  );
}

export function AdminRules({
  scheme,
  published,
}: {
  scheme: R;
  published: () => void;
}) {
  const l = useLoad("/admin/schemes/" + scheme.id + "/rules"),
    [draft, setDraft] = useState<R | null>(null),
    [rule, setRule] = useState(JSON.stringify(defaultRule, null, 2)),
    [error, setError] = useState(""),
    [message, setMessage] = useState("");
  return (
    <>
      <h3>Versioned rules</h3>
      <Load {...l}>
        {l.data?.map((r: R) => (
          <button
            key={r.id}
            onClick={() => {
              setDraft(r);
              setRule(JSON.stringify(r.rule, null, 2));
            }}
          >
            Version {r.id}: {r.status}, revision {r.revision}
          </button>
        ))}
      </Load>
      <button
        onClick={() => {
          setDraft(null);
          setRule(JSON.stringify(defaultRule, null, 2));
        }}
      >
        New rule draft
      </button>
      <form
        onSubmit={async (e) => {
          e.preventDefault();
          try {
            const r = await api(
              "/admin/schemes/" +
                scheme.id +
                "/rules" +
                (draft ? "/" + draft.id : ""),
              draft ? "PUT" : "POST",
              exactRuleBody(rule, draft?.revision),
            );
            setDraft(r);
            setMessage("Rule validated and saved as draft.");
            l.reload();
            setError("");
          } catch (e) {
            setError(err(e));
          }
        }}
      >
        <label className={s.field}>
          Rule JSON
          <textarea
            className={s.code}
            value={rule}
            disabled={draft?.status === "PUBLISHED"}
            onChange={(e) => setRule(e.target.value)}
            spellCheck={false}
          />
        </label>
        <button disabled={draft?.status === "PUBLISHED"}>
          Validate and save draft
        </button>
      </form>
      <button
        disabled={!draft || draft.status !== "DRAFT"}
        onClick={async () => {
          try {
            await api("/admin/schemes/" + scheme.id + "/publish", "POST", {
              draftVersionId: draft?.id,
              expectedDraftRevision: draft?.revision,
            });
            setMessage("Published atomically. This version is immutable.");
            setDraft(null);
            l.reload();
            published();
            setError("");
          } catch (e) {
            setError(err(e));
          }
        }}
      >
        Publish saved draft
      </button>
      <button onClick={l.reload}>Reload versions</button>
      {error && <p role="alert">{error}</p>}
      {message && <p role="status">{message}</p>}
    </>
  );
}

export function AdminQuestions() {
  const l = useLoad("/admin/questions"),
    empty = { label: "", type: "TEXT", active: true, revision: "0" },
    [q, setQ] = useState<R>(empty),
    [options, setOptions] = useState("[]"),
    [error, setError] = useState(""),
    [message, setMessage] = useState("");
  return (
    <section>
      <h2>Question definitions</h2>
      <button onClick={l.reload}>Reload questions</button>
      <p>
        Published references are immutable in meaning. Create a new question for
        a different definition.
      </p>
      <Load {...l}>
        {l.data?.map((x: R) => (
          <button
            key={x.id}
            onClick={() => {
              setQ(x);
              setOptions(JSON.stringify(x.options));
            }}
          >
            {x.id}: {x.label}
          </button>
        ))}
      </Load>
      <button
        onClick={() => {
          setQ(empty);
          setOptions("[]");
        }}
      >
        New question
      </button>
      <form
        onSubmit={async (e) => {
          e.preventDefault();
          try {
            const savedQuestion = await api(
              "/admin/questions" + (q.id ? "/" + q.id : ""),
              q.id ? "PUT" : "POST",
              {
                ...q,
                options: JSON.parse(options),
                expectedRevision: q.revision,
              },
            );
            setQ(savedQuestion);
            setOptions(JSON.stringify(savedQuestion.options));
            l.reload();
            setError("");
            setMessage("Question saved.");
          } catch (e) {
            setError(err(e));
          }
        }}
      >
        <Field
          label="Question label"
          value={q.label}
          set={(v) => setQ({ ...q, label: v })}
          required
        />
        <Field
          label="Answer type"
          value={q.type}
          set={(v) => setQ({ ...q, type: v })}
          options={[
            "TEXT",
            "NUMBER",
            "DATE",
            "BOOLEAN",
            "SELECT",
            "MULTI_SELECT",
          ].map((x) => [x, x])}
        />
        <Field
          label="Options (JSON string array)"
          type="textarea"
          value={options}
          set={setOptions}
        />
        <label>
          <input
            type="checkbox"
            checked={q.active}
            onChange={(e) => setQ({ ...q, active: e.target.checked })}
          />
          Active
        </label>
        <button>Save question</button>
        {error && <p role="alert">{error}</p>}
        {message && <p role="status">{message}</p>}
      </form>
    </section>
  );
}

function AdminReviews() {
  const [page, setPage] = useState(0),
    l = useLoad("/admin/suggestions?page=" + page + "&size=10"),
    [selected, setSelected] = useState<R | null>(null),
    [status, setStatus] = useState("UNDER_REVIEW"),
    [notes, setNotes] = useState(""),
    [error, setError] = useState(""),
    [message, setMessage] = useState(""),
    [conversion, setConversion] = useState("{}");
  return (
    <section>
      <h2>Suggestion review</h2>
      <button onClick={l.reload}>Reload reviews</button>
      <Load {...l}>
        {!l.data?.items.length ? (
          <p>No suggestions to review.</p>
        ) : (
          l.data.items.map((x: R) => (
            <section key={x.id}>
              <h3>{x.schemeName || x.type + " for scheme " + x.schemeId}</h3>
              <p>{x.description}</p>
              <p>{x.status}</p>
              <button
                onClick={async () => {
                  try {
                    const d = await api("/admin/suggestions/" + x.id);
                    setSelected(d);
                    setStatus(
                      d.status === "PENDING" ? "UNDER_REVIEW" : d.status,
                    );
                    setNotes(d.reviewNotes || "");
                    setError("");
                  } catch (e) {
                    setError(err(e));
                  }
                }}
              >
                Review details
              </button>
            </section>
          ))
        )}
        <Pager page={page} total={l.data?.total || 0} set={setPage} />
      </Load>
      {selected && (
        <>
          <h3>Review suggestion {selected.id}</h3>
          <p>{selected.description}</p>
          <p>{selected.proposedCriterion}</p>
          {selected.sourceUrl && (
            <a
              href={selected.sourceUrl}
              target="_blank"
              rel="noopener noreferrer"
            >
              Submitted source
            </a>
          )}
          <form
            onSubmit={async (e) => {
              e.preventDefault();
              try {
                const d = await api(
                  "/admin/suggestions/" + selected.id + "/review",
                  "POST",
                  {
                    status,
                    reviewNotes: notes,
                    expectedRevision: selected.revision,
                  },
                );
                setSelected(d);
                l.reload();
                setMessage("Review saved.");
                setError("");
              } catch (e) {
                setError(err(e));
              }
            }}
          >
            <Field
              label="Review status"
              value={status}
              set={setStatus}
              options={[
                "UNDER_REVIEW",
                "NEEDS_INFO",
                "ACCEPTED",
                "REJECTED",
              ].map((x) => [x, x])}
            />
            <Field
              label="Review notes"
              type="textarea"
              value={notes}
              set={setNotes}
              required
            />
            <button>Save review</button>
          </form>
          {selected.status === "ACCEPTED" && (
            <form
              onSubmit={async (e) => {
                e.preventDefault();
                try {
                  await api(
                    "/admin/suggestions/" + selected.id + "/convert",
                    "POST",
                    selected.type === "CRITERION" ? new ExactJsonBody(conversion) : JSON.parse(conversion),
                  );
                  setMessage(
                    "Draft created. Review it in Schemes before publication.",
                  );
                  setError("");
                } catch (e) {
                  setError(err(e));
                }
              }}
            >
              <h4>Convert accepted suggestion to a draft</h4>
              <p>
                {selected.type === "CRITERION"
                  ? 'Provide {"rule": ...} to create a new draft rule.'
                  : "Provide metadata JSON with name, categoryId, description, benefits, guidance, synthetic and optional source verification fields."}
              </p>
              <Field
                label="Draft JSON"
                type="textarea"
                value={conversion}
                set={setConversion}
              />
              <button>Create draft from suggestion</button>
            </form>
          )}
        </>
      )}
      {error && <p role="alert">{error}</p>}
      {message && <p role="status">{message}</p>}
    </section>
  );
}

function AdminAudit() {
  const [page, setPage] = useState(0),
    l = useLoad("/admin/audit-logs?page=" + page + "&size=20");
  return (
    <section>
      <h2>Audit log</h2>
      <Load {...l}>
        <div className={s.tableWrap}>
          <table>
            <thead>
              <tr>
                <th>Time (UTC)</th>
                <th>Actor</th>
                <th>Action</th>
                <th>Target</th>
              </tr>
            </thead>
            <tbody>
              {l.data?.items.map((x: R) => (
                <tr key={x.id}>
                  <td>{x.createdAt}</td>
                  <td>{x.actorId}</td>
                  <td>{x.action}</td>
                  <td>{x.targetId}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
        <Pager page={page} total={l.data?.total || 0} size={20} set={setPage} />
      </Load>
    </section>
  );
}

const root = document.getElementById("root");
if (root)
  createRoot(root).render(
    <React.StrictMode>
      <App />
    </React.StrictMode>,
  );
