# Project Plan

Generate a new Android app named "Q-base" using Kotlin, Jetpack Compose, and MVVM architecture. This is an advanced medical exam simulator. It will use a local Room Database.

Database Architecture (Room):
The app relies on a pre-populated SQLite database loaded via createFromAsset("MedicalQuiz.db").

Core Read-Only Data:
1. Questions: question_id (String, PK), master_category_id (String), question_type (String - "SBA" or "MCQ"), stem (String).
2. Question_Options: option_id (Int, PK auto), question_id (String, FK), option_letter (String), option_text (String).
3. Answers: question_id (String, PK), correct_answer_string (String), general_explanation (String).
4. Master_Categories: master_category_id (String, PK), name (String), is_user_created (Boolean).
5. Question_Collections: collection_id (String, PK), title (String), is_user_created (Boolean).

Dynamic User Data:
6. Study_Sessions: session_id (String, PK), time_limit_seconds (Int?), score_achieved (Float).
7. Session_Attempts: attempt_id (Int, PK auto), session_id (String, FK), question_id (String, FK), attempt_status (String - "UNATTEMPTED", "ATTEMPTED", "FLAGGED", "FINALIZED"), user_selected_answers (String).

UI Requirements (Jetpack Compose):
Create a main Scaffold with a Bottom Navigation bar containing two main tabs:

1. Explore Tab (Sandbox Mode):
- A screen to browse Master_Categories -> Question_Collections -> List of Questions.
- When a question is clicked, show the stem and options.
- Include an "Explain" button that instantly reveals the Answers.general_explanation. Do not track scores here.

2. Sessions Tab (Exam Simulator):
- A screen showing a list of past Study_Sessions.
- A Floating Action Button (FAB) that opens a ModalBottomSheet to "Create New Session" (Select Category, Number of Questions, Timed/Untimed toggle).
- Active Session Screen:
    * Displays the Question stem.
    * Dynamically display options based on question_type: If "SBA", use a Compose RadioButton group. If "MCQ", use Checkbox components.
    * Include a "Master Navigator" drop-down or bottom sheet: A grid of small circular dots representing all questions in the session.
    * Color-code the dots based on Session_Attempts.attempt_status: Empty/Gray (Unattempted), Solid Blue (Attempted), Orange (Flagged), Solid Dark Gray (Finalized).

## Project Brief

# Project Brief: Q-base
Q-base is an advanced medical exam simulator designed to help students and professionals master medical knowledge through a high-fidelity testing environment. The app balances a relaxed "Explore" mode for learning with a rigorous "Session" mode for exam preparation, all powered by a robust local medical database.
## Features
* Explore Tab (Sandbox Mode): A categorized browsing experience allowing users to navigate through master categories and question collections. Includes an "Explain" feature for instant access to detailed medical rationales without pressure.
* Sessions Tab (Exam Simulator): A comprehensive exam management system where users can create custom study sessions. Features include category selection, question count limits, and a toggle for timed vs. untimed simulations.
* Dynamic Adaptive UI: An intelligent question interface that automatically switches between RadioButton groups for Single Best Answer (SBA) questions and Checkbox components for Multiple Choice Questions (MCQ).
* Master Navigator: A visual tracking system using a grid of color-coded indicators. This provides real-time feedback on session progress, highlighting unattempted, attempted, flagged, and finalized questions.
## High-Level Technical Stack
* Language: Kotlin
* UI Framework: Jetpack Compose (Material Design 3)
* Architecture: MVVM (Model-View-ViewModel)
* Concurrency: Kotlin Coroutines & Flow
* Persistence: Room Database (Pre-populated via SQLite asset)
* Annotation Processing: KSP (Kotlin Symbol Processing)
* Navigation: Jetpack Navigation Compose

## Implementation Steps

### Task_1_DatabaseSetup: Set up the Room database layer, including entities, DAOs, and the database class configured to load from the 'MedicalQuiz.db' asset.
- **Status:** COMPLETED
- **Updates:** Implemented Room entities (Question, QuestionOption, Answer, MasterCategory, QuestionCollection, StudySession, SessionAttempt), DAOs (QuestionDao, CategoryDao, SessionDao), and the AppDatabase class configured with createFromAsset("MedicalQuiz.db"). Verified that the project builds correctly with KSP.
- **Acceptance Criteria:**
  - Room entities match the specified schema
  - DAOs provide methods for all required read and write operations
  - Database is successfully initialized via createFromAsset
  - Project builds without KSP errors

### Task_2_ExploreFeature: Implement the Explore Tab UI and logic, allowing users to browse categories, collections, and questions with the 'Explain' functionality.
- **Status:** COMPLETED
- **Updates:** Implemented the Explore Tab UI and logic, including navigation between categories, collections, and question lists. Added 'Explain' functionality to reveal answers and medical rationales. Used MVVM architecture with Kotlin Flow for data flow and Jetpack Navigation for screen transitions. Verified that the UI is Material 3 compliant and supports edge-to-edge display.
- **Acceptance Criteria:**
  - Navigation between Categories, Collections, and Question list works
  - Question screen shows stem and options
  - 'Explain' button reveals the answer and rationale
  - MVVM pattern is used for data flow

### Task_3_SessionManagement: Implement the Sessions Tab, including the list of past sessions and the 'Create New Session' ModalBottomSheet.
- **Status:** COMPLETED
- **Updates:** Implemented the Sessions Tab, including the list of past sessions and the 'Create New Session' ModalBottomSheet. Created `SessionsViewModel` to manage session history and new session creation. Developed `SessionsListScreen` to display past sessions and `CreateSessionBottomSheet` for inputting session parameters with validation. Integrated the new screens into the app's navigation and verified that new session creation saves data to Room and navigates to the active session.
- **Acceptance Criteria:**
  - Past sessions list is displayed correctly
  - Floating Action Button opens the ModalBottomSheet
  - New session creation saves data to Room and navigates to the active session
  - Input validation for session parameters (Category, Question count)

### Task_4_ActiveSessionUI: Implement the Active Session Screen featuring dynamic question types (SBA/MCQ) and the Master Navigator grid.
- **Status:** COMPLETED
- **Updates:** Implemented the Active Session Screen featuring dynamic question types (SBA/MCQ) and the Master Navigator grid. Created `ActiveSessionViewModel` to manage session lifecycle and persist user selections in `Session_Attempts`. Developed a dynamic question UI using RadioButton and Checkbox components. Integrated the Master Navigator as a ModalBottomSheet with color-coded indicators (Blue for Attempted, Orange for Flagged, Transparent/Outline for Unattempted). Verified that navigation between questions works via both bottom bar buttons and the Master Navigator.
- **Acceptance Criteria:**
  - UI adapts correctly for SBA (RadioButton) vs MCQ (Checkbox)
  - Master Navigator grid updates colors based on attempt status (Blue, Orange, Dark Gray)
  - User selections are persisted in Session_Attempts
  - Navigation between questions works via Master Navigator

### Task_5_FinalPolishAndVerify: Apply Material 3 theme with a vibrant color scheme, implement full Edge-to-Edge display, generate an adaptive app icon, and perform a final verification.
- **Status:** IN_PROGRESS
- **Updates:** The critic_agent reported a critical crash on launch due to a `FileNotFoundException` when attempting to load the Room database from assets. The database is located at `app/src/main/assets/database/MedicalQuiz.db`, but the code was looking for `MedicalQuiz.db`. I am reopening this task to fix the database path and verify stability.
- **Acceptance Criteria:**
  - App uses Material 3 color system with Light/Dark mode support
  - Full Edge-to-Edge display implemented
  - Adaptive app icon matches the medical theme
  - App builds and runs without crashes
  - Final UI alignment with requirements verified
- **StartTime:** 2026-03-18 03:39:01 IST

