# Q-BASE APPLICATION SCREENS DOCUMENTATION

This document provides a comprehensive list of all screens used in the Q-base app, detailing their sections and key components.

---

## 1. Home Screen (`HomeScreen.kt`)
The central hub of the application, prioritizing quick access to study material and recent activities.
- **Sections:**
    - **Top Bar:** Pinned title and user profile access.
    - **Quick Actions:** High-priority buttons for AI Search, Scanner, and Manual Builder.
    - **Collections Category:** Horizontal list of medical categories (Heart, Brain, etc.).
    - **Ongoing Sessions:** List of active, unfinished study sessions.
    - **Pinned Questions:** Vertical list of questions marked for quick review.
- **Key Components:**
    - `QuickActionCard`: Large, colorful cards for primary entry points.
    - `HomeCategoryCard`: Categorized entry points for the Explore flow.
    - `SessionCard`: Compact preview for resume-able sessions.
    - `PinnedQuestionItem`: Minimalist view for bookmarked content.

## 2. Explore Screens (`ExploreScreens.kt`)
Used for browsing and viewing specific question sets.
- **Collection List Screen:**
    - **Sections:** Searchable list of all available question collections.
    - **Components:** `CollectionListItem`, `FilterChip`, `LargeTopAppBar`.
- **Explore Question Pager Screen:**
    - **Sections:** Full-screen question viewing with swipable navigation.
    - **Components:** `QuestionViewer`, `AiAssistanceDialog`, `ReportDialog`.

## 3. Collections & Sets (`CollectionsScreen.kt`, `CollectionOverviewScreen.kt`)
Management of user-created and downloaded question libraries.
- **Collections Screen:**
    - **Sections:** Top Bar (selection/edit mode), Library List, Add Floating Action Button.
    - **Components:** `CollectionItem` (with folder-like UI), `EmptyCollectionsView`.
- **Collection Overview Screen:**
    - **Sections:** Collection Header (metadata), "Pick Up" CTA, Stats Overview (Total/Attempted/Accuracy), Question Sets List.
    - **Components:** `StatCard`, `SetItem`.

## 4. Manual Builder (`ManualBuilderScreen.kt`)
Flow for manually creating or editing question sets.
- **Sections:** Top Bar (Finish action), Search/Filter, Question List, Add Question (FAB).
- **Components:** `ManualQuestionItem`, `ManualBuilderTopBar`.

## 5. Session & Study Flow (`SessionsScreens.kt`, `ActiveSessionScreen.kt`)
The core learning engine of the app.
- **Sessions List Screen:**
    - **Sections:** Past Performance Hub, Session Management, "Start New Session" (FAB).
    - **Components:** `SessionListItemExpressive`, `CategoryChip`.
- **New Session Wizard (Bottom Sheet):**
    - **Steps:** Category/Filter Selection -> Question Count/Selection -> Configuration (Order/Timer).
    - **Components:** `ConfigToggle`, `ValueSlider`.
- **Active Session Screen:**
    - **Sections:** Timer Bar, Progress Tracker (Flag/Navigator icons), Pager (Question content), Navigation Bar (Prev/Next).
    - **Components:** `MasterNavigator` (Grid jump-to-question), `QuestionViewer`, `LinearProgressIndicator`.

## 6. Session Results (`SessionResultsScreen.kt`)
Post-study analysis.
- **Sections:** Top Bar (Report Card), Interactive Score Ring (Percentage/Status), Question Breakdown Grid (Tap-to-review).
- **Components:** `AnimatedAttemptDot` (Color-coded by performance), `ResultsSummaryCard`.

## 7. Chat & Collaboration (`ChatListScreen.kt`, `ChatDetailScreen.kt`)
Collaborative features and AI assistant interface.
- **Chat List Screen:**
    - **Sections:** AI Assistant Quick Entry, Conversation List, New Chat (FAB).
    - **Components:** `AiChatQuickAction`, `ChatItem` (Avatar with status indicator), `EmptyConnectView`.
- **Chat Detail Screen:**
    - **Sections:** Contact Info Header, Message Thread (System/User/AI), Interactive Input Bar (Attachment/Text/Send).
    - **Components:** `MessageBubble` (Tailored for AI/User), `CollectionShareCard`, `SessionInviteCard`.

## 8. User Profile & Settings (`ProfileScreen.kt`, `SettingsScreen.kt`)
User management and global configuration.
- **Profile Screen:**
    - **Sections:** Avatar Header, User Info, Global Stats (Questions Created/Shared), Friend Code Card.
    - **Components:** `StatCard`, `FriendCodeCard`.
- **Settings Screen:**
    - **Sections:** Account, Preferences (Theme/Notifications), AI Engine Configuration (Per-task model selection), Telemetry (Usage stats), Data & Privacy, Support.
    - **Components:** `SettingsToggleCard`, `UsageStatsCard`, `AiConfigSelector`.
