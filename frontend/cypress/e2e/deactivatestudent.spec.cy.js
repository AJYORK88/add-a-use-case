// UC-STU-deactivate-student: the Deactivate action on the edit-student form (steps 3 to 6, extensions 3a and 4a), the
// Deactivated label on the instructor pages, and the student pages that must never show one (CO-ferpa).
// The calls these tests are about are stubbed, so they check what the pages do with each answer and what they send;
// the backend's own tests cover who may deactivate and what is recorded.
// Run against the dev stack (backend + `npm run dev`) with `npm run cy:open`.

const john = {
  id: 4,
  username: 'j.smith@abc.edu',
  firstName: 'John',
  lastName: 'Smith',
  email: 'j.smith@abc.edu',
  enabled: true,
  roles: 'student',
  sectionId: 2,
  sectionName: '2023-2024',
  teamId: 1,
  teamName: 'Team1'
}
const deactivatedJohn = { ...john, enabled: false }
const ok = (data, message = 'OK') => ({ flag: true, code: 200, message, data })
const page = (content) =>
  ok({
    content,
    totalElements: content.length,
    totalPages: 1,
    size: 100,
    number: 0,
    first: true,
    last: true,
    empty: false
  })

describe('Deactivate a student from the edit-student form', () => {
  beforeEach(() => {
    cy.login('b.gates@abc.edu', '123456')
    cy.intercept('POST', '**/students/search*', { body: page([john]) }).as('search')
  })

  function openEditForm(studentBody) {
    cy.intercept('GET', '**/students/4', { body: ok(studentBody) }).as('student')
    cy.visit('/students')
    cy.wait('@search')
    cy.get('[data-cy=student-edit]').first().click()
    cy.wait('@student')
  }

  it('offers Deactivate only while the student is active', () => {
    openEditForm(john)
    cy.get('[data-cy=student-status]').should('have.text', 'Active')
    cy.get('[data-cy=student-deactivate]').should('be.visible')
  })

  it('shows a deactivated student as Deactivated, with no Deactivate button', () => {
    openEditForm(deactivatedJohn)
    cy.get('[data-cy=student-status]').should('have.text', 'Deactivated')
    cy.get('[data-cy=student-deactivate]').should('not.exist')
  })

  it('warns of the consequences, and Cancel sends nothing (4a)', () => {
    cy.intercept('POST', '**/students/4/deactivation', cy.spy().as('deactivation'))
    openEditForm(john)

    cy.get('[data-cy=student-deactivate]').click()
    cy.get('.el-message-box')
      .should('contain', 'Deactivate John Smith?')
      .and(
        'contain',
        'will no longer be able to log in or submit weekly activity reports and peer evaluations'
      )
      .and('contain', 'The account and all submitted work are kept')
      .and('contain', 'stays on the same team and course section')
      .and('not.contain', 'reactivated') // Not promised until UC-STU-reactivate-student exists (OI-67)
    cy.get('.el-message-box').contains('button', 'Cancel').click()

    cy.get('@deactivation').should('not.have.been.called')
    cy.get('[data-cy=student-status]').should('have.text', 'Active')
  })

  it('deactivates on Confirm with exactly one request, and shows the student as Deactivated (steps 5 and 6)', () => {
    cy.intercept('POST', '**/students/4/deactivation', {
      delay: 500,
      body: ok(deactivatedJohn, 'Deactivate student successfully')
    }).as('deactivation')
    openEditForm(john)

    cy.get('[data-cy=student-deactivate]').click()
    cy.get('.el-message-box').contains('button', 'Deactivate').click()

    cy.wait('@deactivation').its('request.body').should('be.empty')
    cy.get('.el-message--success').should('contain', 'John Smith has been deactivated.')
    cy.get('[data-cy=student-status]').should('have.text', 'Deactivated')
    cy.get('[data-cy=student-deactivate]').should('not.exist')
    cy.get('@deactivation.all').should('have.length', 1)
  })

  it('reports an already-deactivated student once, then shows her as Deactivated (3a)', () => {
    cy.intercept('POST', '**/students/4/deactivation', {
      statusCode: 409,
      body: { flag: false, code: 409, message: 'This student is already deactivated.' }
    }).as('deactivation')
    openEditForm(john)
    // Someone else deactivated her while the form was open, so the reload returns her deactivated
    cy.intercept('GET', '**/students/4', { body: ok(deactivatedJohn) }).as('reload')

    cy.get('[data-cy=student-deactivate]').click()
    cy.get('.el-message-box').contains('button', 'Deactivate').click()

    cy.wait('@deactivation')
    cy.wait('@reload')
    cy.get('.el-message--error')
      .should('have.length', 1)
      .and('contain', 'This student is already deactivated.')
    cy.get('[data-cy=student-status]').should('have.text', 'Deactivated')
    cy.get('[data-cy=student-deactivate]').should('not.exist')
  })
})

// The course section's evaluation report for W32, with John deactivated since and Eric still active
function expectTheReportToMarkJohnDeactivated() {
  const average = (studentId, firstName, lastName, deactivated) => ({
    studentId,
    week: '2023-W32',
    firstName,
    lastName,
    email: `${firstName}@abc.edu`,
    teamName: 'Team1',
    deactivated,
    averageTotalScore: 50,
    publicComments: [],
    privateComments: [],
    ratingAverages: []
  })
  cy.intercept('GET', '**/evaluations/sections/*/week/*', {
    body: ok({
      sectionName: '2023-2024',
      week: '2023-W32',
      peerEvaluationAverages: [
        average(4, 'John', 'Smith', true),
        average(5, 'Eric', 'Hudson', false)
      ]
    })
  }).as('report')
  cy.visit('/section-evaluations')
  cy.wait('@report')
  cy.contains('tr', 'Smith, John')
    .find('[data-cy=member-deactivated]')
    .should('have.text', 'Deactivated')
  cy.contains('tr', 'Hudson, Eric').find('[data-cy=member-deactivated]').should('not.exist')
}

describe('The Deactivated label on instructor pages', () => {
  beforeEach(() => {
    cy.login('b.wei@abc.edu', '123456')
  })

  it('marks a deactivated team member on Teams', () => {
    // The real roster, with John deactivated
    cy.intercept('POST', '**/students/search*', (req) =>
      req.continue((res) => {
        res.body.data.content.forEach((student) => {
          if (student.id === 4) student.enabled = false
        })
      })
    ).as('roster')
    cy.visit('/teams')
    cy.wait('@roster')
    // Teams.vue renders every team expanded (default-expand-all), so Team1's members are on the page already
    cy.get('[data-team-id=1]').within(() => {
      cy.contains('.trello-card', 'John Smith')
        .find('[data-cy=member-deactivated]')
        .should('have.text', 'Deactivated')
      cy.contains('.trello-card', 'Eric Hudson')
        .find('[data-cy=member-deactivated]')
        .should('not.exist')
    })
  })

  it('marks a deactivated student in the section evaluation report', () => {
    expectTheReportToMarkJohnDeactivated()
  })

  it('marks a deactivated student on the section activities page, from the instructor roster', () => {
    const activity = (activityId, studentId, studentName) => ({
      activityId,
      studentId,
      studentName,
      week: '2023-W32',
      teamId: 1,
      teamName: 'Team1',
      category: 'DEVELOPMENT',
      activity: 'Work',
      description: 'Work',
      plannedHours: 1,
      actualHours: 1,
      status: 'COMPLETED'
    })
    cy.intercept('POST', '**/activities/search*', {
      body: page([activity(1, 4, 'John Smith'), activity(2, 5, 'Eric Hudson')])
    }).as('activities')
    cy.intercept('POST', '**/students/search*', {
      body: page([deactivatedJohn, { ...john, id: 5, firstName: 'Eric', lastName: 'Hudson' }])
    }).as('roster')
    cy.visit('/section-activities')
    cy.wait('@activities')
    cy.wait('@roster')
    cy.get('.el-table__expand-icon').first().click()
    cy.contains('tr', 'John Smith')
      .find('[data-cy=member-deactivated]')
      .should('have.text', 'Deactivated')
    cy.contains('tr', 'Eric Hudson').find('[data-cy=member-deactivated]').should('not.exist')
  })
})

describe('The Deactivated label for an instructor who is not the course admin', () => {
  // Bill is assigned to course section 2 but does not own its course; every test above logs in as Bingyang, who does
  beforeEach(() => {
    cy.login('b.gates@abc.edu', '123456')
  })

  it('marks a deactivated student in the section evaluation report', () => {
    expectTheReportToMarkJohnDeactivated()
  })
})

describe('A student never sees another student’s status (CO-ferpa)', () => {
  beforeEach(() => {
    cy.login('j.smith@abc.edu', '123456')
  })

  it('lists exactly the evaluatees the server sends, with no status', () => {
    cy.intercept('GET', '**/evaluations/weeks/*/evaluatees', {
      body: ok([
        { id: 4, firstName: 'John', lastName: 'Smith' },
        { id: 6, firstName: 'Jerry', lastName: 'Moon' }
      ])
    }).as('evaluatees')
    cy.intercept('GET', '**/evaluations/evaluators/*/week/*', { body: ok([]) })
    cy.intercept('GET', '**/students/teams/*', cy.spy().as('teamMembers'))
    cy.visit('/evaluations/submit-evaluations')
    cy.wait('@evaluatees')

    cy.get('.el-table__body tr').should('have.length', 2)
    cy.get('.el-table__body')
      .should('contain', 'John Smith')
      .and('contain', 'Jerry Moon')
      .and('not.contain', 'Eric Hudson')
    cy.contains('Deactivated').should('not.exist')
    cy.contains('Active').should('not.exist')
    cy.get('@teamMembers').should('not.have.been.called') // The form no longer reads the team list
  })

  it('shows no status on the team activities page', () => {
    // The real backend, asked for W32, where the seed has Team1's activities: the page opens on the browser's own
    // current week, which has none, and an empty page would pass the check below for nothing
    cy.intercept('POST', '**/activities/search*', (req) => {
      req.body.week = '2023-W32'
    }).as('activities')
    cy.visit('/activities/team-activities')
    cy.wait('@activities')
    cy.contains('h3', "John Smith's Weekly Activities").should('be.visible')
    cy.contains('h3', "Eric Hudson's Weekly Activities").should('be.visible')

    cy.get('[data-cy=member-deactivated]').should('not.exist')
    cy.contains('Deactivated').should('not.exist')
  })
})
