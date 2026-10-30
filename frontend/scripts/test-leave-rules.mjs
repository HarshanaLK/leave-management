/** Focused frontend business-rule checks, no browser needed: npm run qa:rules */
import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import { runInNewContext } from "node:vm";
import ts from "typescript";

const source = readFileSync(
  new URL("../src/lib/leaveRules.ts", import.meta.url),
  "utf8",
);
const built = ts.transpileModule(source, {
  compilerOptions: {
    target: ts.ScriptTarget.ES2022,
    module: ts.ModuleKind.CommonJS,
  },
  reportDiagnostics: true,
});
const problems = (built.diagnostics ?? []).filter(
  (d) => d.category === ts.DiagnosticCategory.Error,
);
assert.equal(problems.length, 0, "Leave rules must compile");
const module = { exports: {} };
runInNewContext(built.outputText, { module, exports: module.exports });
const {
  todayInLeaveZone,
  validateLeaveDates,
  addSelectedDocuments,
  removeSelectedDocument,
} = module.exports;

const cases = [
  [
    "Sri Lanka date does not shift backward when UTC is previous calendar day",
    () => {
      assert.equal(
        todayInLeaveZone(new Date("2026-10-28T20:00:00Z")),
        "2026-10-29",
      );
    },
  ],
  [
    "Earlier Sri Lanka time stays on the prior day",
    () => {
      assert.equal(
        todayInLeaveZone(new Date("2026-10-28T18:20:00Z")),
        "2026-10-28",
      );
    },
  ],
  [
    "Past leave date is rejected",
    () => {
      assert.ok(
        validateLeaveDates("2026-10-28", "2026-10-30", "2026-10-29").startDate,
      );
    },
  ],
  [
    "Today is permitted",
    () => {
      assert.equal(
        Object.keys(
          validateLeaveDates("2026-10-29", "2026-10-29", "2026-10-29"),
        ).length,
        0,
      );
    },
  ],
  [
    "Future date range is permitted",
    () => {
      assert.equal(
        Object.keys(
          validateLeaveDates("2026-11-01", "2026-11-03", "2026-10-29"),
        ).length,
        0,
      );
    },
  ],
  [
    "Reversed dates are rejected",
    () => {
      assert.ok(
        validateLeaveDates("2026-11-03", "2026-11-01", "2026-10-29").endDate,
      );
    },
  ],
  [
    "Missing dates require both inputs",
    () => {
      const result = validateLeaveDates("", "", "2026-10-29");
      assert.ok(result.startDate && result.endDate);
    },
  ],
  [
    "Multiple selections append without repeating the same file",
    () => {
      const a = { name: "a.pdf", size: 123, lastModified: 1 };
      const duplicate = { name: "a.pdf", size: 123, lastModified: 1 };
      const b = { name: "b.jpg", size: 456, lastModified: 2 };
      const files = addSelectedDocuments([a], [duplicate, b]);
      assert.equal(files.length, 2);
      assert.equal(files[1].name, "b.jpg");
    },
  ],
  [
    "Removing a selected document keeps the remaining documents",
    () => {
      const files = [{ name: "a.pdf" }, { name: "b.pdf" }, { name: "c.pdf" }];
      const updated = removeSelectedDocument(files, 1);
      assert.equal(updated.length, 2);
      assert.equal(updated[0].name, "a.pdf");
      assert.equal(updated[1].name, "c.pdf");
      assert.equal(files.length, 3);
    },
  ],
];
for (const [name, run] of cases) {
  run();
  console.log("PASS", name);
}
console.log(`Passed ${cases.length} leave-form QA checks.`);
