---
name: material3-design
description: "Material Design 3 specifications, color system, typography, component implementation, and accessibility guidelines. Use when: designing Material 3 UI, implementing theme, choosing components, ensuring design consistency."
applyTo: ["**/*.kt", "**/*.xml", "**/*Composable*"]
---

# Material Design 3 Implementation Guide

## Official Resources

- **Material 3 Web**: https://m3.material.io/
- **Material Components for Android**: https://developer.android.com/design/material
- **Compose Material 3**: https://developer.android.com/reference/kotlin/androidx/compose/material3/package-summary
- **Material Icons**: https://fonts.google.com/icons

## Color System

### Dynamic Color (Material You)
```kotlin
@Composable
fun AppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context)
            else dynamicLightColorScheme(context)
        }
        darkTheme -> darkColorScheme()
        else -> lightColorScheme()
    }
    
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}
```

### Color Tokens (Use instead of hardcoding)
```kotlin
// ✅ Semantic color usage
Surface {
    Text(
        "Hello",
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.background(MaterialTheme.colorScheme.surface)
    )
}

// Available tokens:
// Primary, Secondary, Tertiary, Error
// Variants: Container, OnColor
// Surface, SurfaceVariant, OnSurface
// Background, OnBackground
// Outline, OutlineVariant
```

### Dark Theme
```kotlin
private val darkColorScheme = darkColorScheme(
    primary = md_theme_dark_primary,
    onPrimary = md_theme_dark_onPrimary,
    primaryContainer = md_theme_dark_primaryContainer,
    onPrimaryContainer = md_theme_dark_onPrimaryContainer,
    // ... complete color scheme
)
```

## Typography

### Material 3 Type Scale
```kotlin
val Typography = Typography(
    displayLarge = TextStyle(
        fontSize = 57.sp,
        lineHeight = 64.sp,
        fontWeight = FontWeight.W400,
        letterSpacing = (-0.25).sp
    ),
    displayMedium = TextStyle(
        fontSize = 45.sp,
        lineHeight = 52.sp,
        fontWeight = FontWeight.W400
    ),
    // ... headlineLarge, headlineMedium, headlineSmall
    // ... titleLarge, titleMedium, titleSmall
    // ... bodyLarge, bodyMedium, bodySmall
    // ... labelLarge, labelMedium, labelSmall
)

// Usage:
Text("Headline", style = MaterialTheme.typography.headlineLarge)
Text("Body text", style = MaterialTheme.typography.bodyMedium)
```

### Type Scale Hierarchy
- **Display**: Large, prominent headings
- **Headline**: Main content sections
- **Title**: Component titles, short headers
- **Body**: Main content, longer text
- **Label**: Small UI labels, buttons

## Components

### Buttons

```kotlin
// Filled Button (primary action)
Button(onClick = { }) {
    Text("Save")
}

// Filled Tonal Button (secondary action)
FilledTonalButton(onClick = { }) {
    Text("Create")
}

// Outlined Button (tertiary action)
OutlinedButton(onClick = { }) {
    Text("Cancel")
}

// Text Button (lowest priority)
TextButton(onClick = { }) {
    Text("Learn More")
}

// Elevated Button (raised state)
ElevatedButton(onClick = { }) {
    Text("Upload")
}
```

### Text Fields

```kotlin
// Outlined TextField (preferred)
var name by remember { mutableStateOf("") }
OutlinedTextField(
    value = name,
    onValueChange = { name = it },
    label = { Text("Name") },
    modifier = Modifier.fillMaxWidth()
)

// Filled TextField (alternative)
FilledTextField(
    value = name,
    onValueChange = { name = it },
    label = { Text("Name") }
)
```

### Cards

```kotlin
// Elevated Card (with shadow)
ElevatedCard(
    modifier = Modifier.fillMaxWidth()
) {
    Column(modifier = Modifier.padding(16.dp)) {
        Text("Card Title", style = MaterialTheme.typography.headlineMedium)
        Text("Card content", style = MaterialTheme.typography.bodyMedium)
    }
}

// Filled Card (no elevation)
Card(
    modifier = Modifier.fillMaxWidth(),
    colors = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surfaceVariant
    )
) {
    // Content
}

// Outlined Card (with border)
OutlinedCard(modifier = Modifier.fillMaxWidth()) {
    // Content
}
```

### Lists & Chips

```kotlin
// List Items (with Material 3 spacing)
ListItem(
    headlineContent = { Text("Primary text") },
    supportingContent = { Text("Secondary text") },
    leadingContent = {
        Icon(Icons.Default.Home, contentDescription = null)
    }
)

// Filter Chip
var selected by remember { mutableStateOf(false) }
FilterChip(
    selected = selected,
    onClick = { selected = !selected },
    label = { Text("Filter") }
)

// Input Chip
InputChip(
    selected = true,
    onClick = { },
    label = { Text("Tag") },
    avatar = {
        Icon(Icons.Default.Close, contentDescription = null)
    }
)
```

### Navigation

```kotlin
// NavigationBar (bottom navigation)
NavigationBar {
    NavigationBarItem(
        selected = selectedItem == 0,
        onClick = { selectedItem = 0 },
        icon = { Icon(Icons.Default.Home, contentDescription = null) },
        label = { Text("Home") }
    )
}

// NavigationRail (side navigation)
NavigationRail {
    NavigationRailItem(
        selected = selectedItem == 0,
        onClick = { selectedItem = 0 },
        icon = { Icon(Icons.Default.Home, contentDescription = null) },
        label = { Text("Home") }
    )
}

// Navigation Drawer
ModalNavigationDrawer(
    drawerContent = {
        ModalDrawerSheet {
            NavigationDrawerItem(
                label = { Text("Home") },
                selected = selectedItem == 0,
                onClick = { selectedItem = 0 }
            )
        }
    }
) {
    // Main content
}
```

## Spacing & Layout

### Material 3 Spacing Scale (4dp grid)
```kotlin
// Standard spacing values
val spacing_2xs = 4.dp    // Minimal gaps
val spacing_xs = 8.dp     // Tight spacing
val spacing_sm = 12.dp    // Small spacing
val spacing_md = 16.dp    // Default spacing
val spacing_lg = 24.dp    // Large spacing
val spacing_xl = 32.dp    // Extra large spacing
val spacing_xxl = 48.dp   // Maximum spacing

// Usage
Spacer(modifier = Modifier.height(16.dp))
Column(modifier = Modifier.padding(16.dp)) { }
Row(modifier = Modifier.padding(horizontal = 12.dp)) { }
```

### Common Layouts
```kotlin
// Full-width button
Button(
    onClick = { },
    modifier = Modifier.fillMaxWidth()
) {
    Text("Full Width")
}

// Constrained layout
Column(
    modifier = Modifier
        .fillMaxWidth(0.8f)
        .align(Alignment.CenterHorizontally)
) { }

// Responsive padding
val padding = when {
    screenWidth < 600.dp -> 16.dp
    else -> 24.dp
}
```

## State Layers

Material 3 uses state layers for interactive components:
- **Hover**: 8% opacity of foreground color
- **Focus**: 12% opacity of foreground color
- **Pressed**: 12% opacity
- **Drag**: 16% opacity

These are handled automatically by Material 3 components.

## Shapes & Elevation

### Corner Radius
```kotlin
val shapes = Shapes(
    extraLarge = RoundedCornerShape(28.dp),  // dialogs, bottom sheets
    large = RoundedCornerShape(16.dp),       // cards, menus
    medium = RoundedCornerShape(12.dp),      // buttons, text fields
    small = RoundedCornerShape(8.dp),        // small components
    extraSmall = RoundedCornerShape(4.dp)    // minimal rounding
)
```

### Elevation
```kotlin
// Surface with elevation
Surface(
    modifier = Modifier.fillMaxWidth(),
    shadowElevation = 6.dp,
    shape = RoundedCornerShape(12.dp)
) {
    // Content
}

// Common elevation values
// 0dp - flat (default)
// 1dp - text, disabled
// 3dp - buttons, chips
// 6dp - cards, menus
// 8dp - dialogs, sheets
```

## Accessibility

### Color Contrast
- Minimum WCAG AA: 4.5:1 for normal text, 3:1 for large text
- Material 3 color pairings meet accessibility standards
- Always provide text labels for icons

### Touch Target Size
```kotlin
// Minimum 48dp for touch targets
Button(
    onClick = { },
    modifier = Modifier.size(48.dp)
)

// Use semantics for screen readers
Icon(
    imageVector = Icons.Default.Close,
    contentDescription = "Close dialog",
    modifier = Modifier.size(24.dp)
)
```

### Content Description
```kotlin
// Always provide meaningful descriptions
Icon(
    imageVector = Icons.Default.Home,
    contentDescription = "Navigate to home",
    tint = MaterialTheme.colorScheme.primary
)

// Decorative icons
Icon(
    imageVector = Icons.Default.Star,
    contentDescription = null,  // Decorative only
    modifier = Modifier.size(16.dp)
)
```

## Motion & Animation

### Transitions
```kotlin
// Simple color transition
val backgroundColor by animateColorAsState(
    targetValue = if (isSelected) 
        MaterialTheme.colorScheme.primary 
    else 
        MaterialTheme.colorScheme.surface,
    label = "backgroundColor"
)

// State animation
AnimatedVisibility(
    visible = showContent,
    enter = expandVertically(),
    exit = shrinkVertically()
) {
    Text("Content")
}
```

### Motion Tokens (Material 3)
- Short: 100ms (small, quick interactions)
- Medium: 300ms (most interactions)
- Long: 500ms (complex animations)

## Dark Mode Support

```kotlin
// Automatically handle dark mode
val backgroundColor = MaterialTheme.colorScheme.background
val textColor = MaterialTheme.colorScheme.onBackground

// Or explicitly check
val isDarkMode = isSystemInDarkTheme()
Text(
    "Text",
    color = if (isDarkMode) 
        MaterialTheme.colorScheme.onSurfaceVariant 
    else 
        MaterialTheme.colorScheme.onSurface
)
```

## Common Patterns

### Bottom Sheet
```kotlin
var showBottomSheet by remember { mutableStateOf(false) }

ModalBottomSheet(
    onDismissRequest = { showBottomSheet = false }
) {
    Column(modifier = Modifier.padding(16.dp)) {
        Text("Sheet Title", style = MaterialTheme.typography.headlineMedium)
        // Sheet content
    }
}
```

### Dialog
```kotlin
if (showDialog) {
    AlertDialog(
        onDismissRequest = { showDialog = false },
        title = { Text("Confirm") },
        text = { Text("Are you sure?") },
        confirmButton = {
            TextButton(onClick = { showDialog = false }) {
                Text("Yes")
            }
        },
        dismissButton = {
            TextButton(onClick = { showDialog = false }) {
                Text("No")
            }
        }
    )
}
```

### Snackbar
```kotlin
val snackbarHostState = remember { SnackbarHostState() }

LaunchedEffect(Unit) {
    snackbarHostState.showSnackbar("Action completed")
}

Scaffold(
    snackbarHost = { SnackbarHost(snackbarHostState) }
) {
    // Content
}
```

## Design Checklist

- ✅ Using Material 3 color scheme (not hardcoded colors)
- ✅ Typography follows Material 3 scale
- ✅ Spacing aligns to 4dp grid
- ✅ Touch targets minimum 48dp
- ✅ Color contrast WCAG AA compliant
- ✅ Icons have content descriptions
- ✅ Dark mode supported
- ✅ Animations follow motion tokens
- ✅ Shapes use consistent corner radius
- ✅ Elevation indicates hierarchy
