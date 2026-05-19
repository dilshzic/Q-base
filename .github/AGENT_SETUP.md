# Android Development Custom Agent Setup

This directory contains VS Code Copilot customizations for specialized Android native development.

## 📋 Files Created

### 1. **Custom Agent: `.github/agents/android-dev.agent.md`**
   - **Name**: `AndroidDevExpert`
   - **Purpose**: Specialized agent for all native Android development tasks
   - **Activation**: Triggered when working with Kotlin, Java, XML, Gradle files
   - **Expertise Areas**:
     - Jetpack Compose & Material 3 UI
     - Kotlin & modern Android architecture
     - Android core capabilities & lifecycle
     - Performance optimization
     - Testing & quality assurance

### 2. **Error Knowledge Base: `.github/instructions/android-errors.instructions.md`**
   - **Purpose**: Comprehensive catalog of common Android errors with solutions
   - **Coverage**:
     - Memory management & leaks (RecyclerView, Context, ViewModel)
     - Fragment & Activity lifecycle issues
     - Threading & concurrency problems
     - Permissions & security
     - Database & Room issues
     - Jetpack Compose recomposition issues
     - Navigation problems
     - Manifest & build issues
     - Performance & ANR prevention
     - Material 3 theming issues
     - Debugging tips & patterns
   - **Auto-loaded** when working with Kotlin/Java files

### 3. **Best Practices: `.github/instructions/android-best-practices.instructions.md`**
   - **Purpose**: General Android development guidelines and conventions
   - **Coverage**:
     - Kotlin code style & conventions
     - Architecture patterns (MVVM, Repository)
     - Dependency injection with Hilt
     - Coroutines & async best practices
     - Jetpack Compose patterns
     - Material 3 guidelines
     - Testing strategies
     - Navigation Component
     - Security & permissions
     - Lifecycle management
   - **Auto-loaded** for all Kotlin files

### 4. **Material 3 Design Guide: `.github/instructions/material3-design.instructions.md`**
   - **Purpose**: Comprehensive Material Design 3 implementation guide
   - **Coverage**:
     - Color system & dynamic color (Material You)
     - Typography & type scale
     - Component implementations (Button, TextField, Card, etc.)
     - Spacing & layout systems
     - State layers & elevation
     - Accessibility requirements
     - Motion & animation
     - Dark mode support
     - Common UI patterns
     - Design checklist
   - **References**: Official m3.material.io with live links

## 🚀 How to Use

### Automatic Activation
The customizations are automatically active when:
- Opening Kotlin (*.kt) files → loads all instructions + agent
- Opening Java (*.java) files → loads agent guidance
- Opening XML (*.xml) files → loads Material 3 design reference
- Opening Gradle files → loads architecture guidance

### Manual Invocation
In VS Code chat, you can explicitly reference:
- **`@AndroidDevExpert`** - Call the specialized agent directly
- **Ask about errors** - Agent will reference error knowledge base
- **Material 3 questions** - Agent loads design guidelines

### Example Conversations
```
"@AndroidDevExpert Review my ViewModel for memory leaks"
→ Agent checks against best practices + error patterns

"Help me implement a Material 3 bottom sheet"
→ Agent provides design + code examples with live M3 links

"Why is my RecyclerView leaking memory?"
→ Agent references error KB + provides solutions

"What's the best way to handle lifecycle-aware coroutines?"
→ Agent explains structured concurrency patterns
```

## 📚 Knowledge Base Features

### Error Tracking
The error knowledge base includes:
- **Root cause analysis** - Why each error occurs
- **Code examples** - ❌ Bad vs ✅ Good patterns
- **Prevention strategies** - How to avoid in future
- **Debugging tips** - How to identify issues
- **Performance insights** - Impact on app performance

### Updating the Error KB
When discovering new Android errors:

1. Add entry to `android-errors.instructions.md` with:
   - Clear error name
   - Root cause explanation
   - Code example (bad vs good)
   - Prevention strategy
   - Links to official docs

2. Format:
```markdown
### New Error Name
**Error**: How it appears in logcat
**Cause**: Why it happens
**Solution**: Code example with ✅ Good pattern
**Prevention**: How to avoid next time
```

## 🔗 Official References

Agent automatically references:
- **Android Developers**: https://developer.android.com/
- **Material Design 3**: https://m3.material.io/
- **Jetpack Compose Docs**: https://developer.android.com/jetpack/compose
- **Android Blog**: https://android-developers.googleblog.com/

## 📋 Supported Android Versions
- Target: Android 12+ (API 31+)
- Minimum: Android 8.0 (API 26) for Q-base project
- Material 3: Full support with dynamic color on API 31+

## 🎨 Project Context

This setup is tailored for the **Q-base** project:
- **UI Framework**: Jetpack Compose + Material 3
- **Architecture**: MVVM with Repository pattern
- **Backend**: Appwrite integration
- **Language**: Kotlin-first
- **Minimum SDK**: 26
- **Target SDK**: 34

## 🔄 Version Updates

The agent and instructions reference **latest official documentation**:
- Updates automatically detect Material 3 releases
- Checks Android API documentation changes
- Jetpack library updates included

Last Updated: 2026-05-19

---

**Next Steps**:
1. Open a Kotlin file in your Android project
2. Ask about a specific Android development task
3. The AndroidDevExpert agent will automatically load relevant knowledge
4. For new errors discovered, update `android-errors.instructions.md`
