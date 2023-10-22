const STEP_TIMEOUT = 250

describe("Email Template Export", () => {
  let lastIteration = -1;

  it("should navigate through all templates and take screenshots", () => {
    cy.visit("http://localhost:9080");

    cy.get("a").each(($link, index, $list) => {
      if (index <= lastIteration) {
        return;
      }

      lastIteration = index;
      const templateName = $link.text().trim()

      cy.visit($link.prop("href"));
      cy.wait(STEP_TIMEOUT);

      cy.screenshot(templateName);
      cy.task('log', `==> ${templateName} Screenshot captured. ${index + 1}/${$list.length}`)

      cy.go("back");
      cy.wait(STEP_TIMEOUT);
    });
  });
});
