---
name: MOVE-AI
colors:
  surface: '#f8f9fa'
  surface-dim: '#d9dadb'
  surface-bright: '#f8f9fa'
  surface-container-lowest: '#ffffff'
  surface-container-low: '#f3f4f5'
  surface-container: '#edeeef'
  surface-container-high: '#e7e8e9'
  surface-container-highest: '#e1e3e4'
  on-surface: '#191c1d'
  on-surface-variant: '#414754'
  inverse-surface: '#2e3132'
  inverse-on-surface: '#f0f1f2'
  outline: '#717786'
  outline-variant: '#c1c6d7'
  surface-tint: '#005bc0'
  primary: '#0059bb'
  on-primary: '#ffffff'
  primary-container: '#0070ea'
  on-primary-container: '#fefcff'
  inverse-primary: '#adc7ff'
  secondary: '#565f6b'
  on-secondary: '#ffffff'
  secondary-container: '#dae3f1'
  on-secondary-container: '#5c6571'
  tertiary: '#755700'
  on-tertiary: '#ffffff'
  tertiary-container: '#946f00'
  on-tertiary-container: '#fffbff'
  error: '#ba1a1a'
  on-error: '#ffffff'
  error-container: '#ffdad6'
  on-error-container: '#93000a'
  primary-fixed: '#d8e2ff'
  primary-fixed-dim: '#adc7ff'
  on-primary-fixed: '#001a41'
  on-primary-fixed-variant: '#004493'
  secondary-fixed: '#dae3f1'
  secondary-fixed-dim: '#bec7d5'
  on-secondary-fixed: '#131c26'
  on-secondary-fixed-variant: '#3f4853'
  tertiary-fixed: '#ffdf9e'
  tertiary-fixed-dim: '#fabd00'
  on-tertiary-fixed: '#261a00'
  on-tertiary-fixed-variant: '#5b4300'
  background: '#f8f9fa'
  on-background: '#191c1d'
  surface-variant: '#e1e3e4'
typography:
  display-lg:
    fontFamily: Inter
    fontSize: 32px
    fontWeight: '800'
    lineHeight: '1.2'
    letterSpacing: -0.02em
  headline-md:
    fontFamily: Inter
    fontSize: 24px
    fontWeight: '700'
    lineHeight: 32px
  headline-sm:
    fontFamily: Inter
    fontSize: 18px
    fontWeight: '600'
    lineHeight: 24px
  body-lg:
    fontFamily: Inter
    fontSize: 16px
    fontWeight: '400'
    lineHeight: 24px
  body-md:
    fontFamily: Inter
    fontSize: 14px
    fontWeight: '400'
    lineHeight: 20px
  label-md:
    fontFamily: Inter
    fontSize: 13px
    fontWeight: '600'
    lineHeight: 16px
    letterSpacing: 0.01em
  label-sm:
    fontFamily: Inter
    fontSize: 12px
    fontWeight: '500'
    lineHeight: 16px
  headline-md-mobile:
    fontFamily: Inter
    fontSize: 20px
    fontWeight: '700'
    lineHeight: 28px
rounded:
  sm: 0.125rem
  DEFAULT: 0.25rem
  md: 0.375rem
  lg: 0.5rem
  xl: 0.75rem
  full: 9999px
spacing:
  container-margin: 24px
  gutter-md: 16px
  sidebar-width: 260px
  header-height: 72px
  table-row-height: 56px
---

## Brand & Style

The design system is engineered for **MOVE-AI**, a high-efficiency logistics administration platform. The brand personality is professional, data-centric, and reliable, prioritizing clarity over decoration to reduce cognitive load for operations managers.

The visual style is **Corporate / Modern**, characterized by a systematic approach to density, a clean "all-white" card aesthetic against soft neutral backgrounds, and a high-energy primary blue to denote action and focus. The interface utilizes subtle borders and consistent horizontal rhythm to organize complex datasets into digestible flows. It aims to evoke a sense of precision, technological sophistication, and operational control.

## Colors

The palette is optimized for long-duration usage in administrative environments.

*   **Primary (#007BFF):** A vibrant "Action Blue" used for branding, active navigation states, primary buttons, and critical interactive elements.
*   **Secondary (#E8F1FF):** A soft tint of the primary blue used for subtle backgrounds in active menu items and highlighted table rows.
*   **Neutral Background (#F8F9FA):** A cool, light gray that provides enough contrast to make white surface cards pop.
*   **Status Colors:** 
    *   **Warning/Pending (#FFC107):** Used for "Waiting" or "Pending" status badges.
    *   **Border/Divider (#DEE2E6):** A consistent hairline gray for table borders and input outlines.
*   **Surface (#FFFFFF):** Pure white is reserved for content containers, cards, and data tables to ensure maximum legibility.

## Typography

This design system utilizes **Inter** as its sole typeface family to maintain a systematic, utilitarian aesthetic. 

- **Weight Usage:** Bold (700/800) is reserved for brand titles and page headers. Semibold (600) is used for table headers and button text. Regular (400) is used for all data entries and descriptive text.
- **Scale:** The hierarchy is tight. Most data resides at the `body-md` (14px) level to maximize information density.
- **Localization:** When rendering CJK characters, ensure line heights are increased by 10% to maintain vertical balance.

## Layout & Spacing

The layout follows a **Fixed Sidebar + Fluid Content** model.

*   **Sidebar:** Fixed at 260px. It contains the primary brand mark and navigation links.
*   **Header:** A persistent top bar (72px height) housing search and global actions.
*   **Grid:** A 12-column system is used within the fluid content area. 
*   **Margins:** A standard 24px margin (safe area) surrounds the main content container.
*   **Density:** The layout is "comfortable-compact." Elements like table rows have a height of 56px to ensure touch targets are sufficient while displaying multiple data rows.
*   **Responsive:** On tablets, the sidebar collapses into a hamburger menu. On mobile, the filter bar reflows from a single horizontal row into a stacked vertical list.

## Elevation & Depth

This design system uses **Tonal Layers** and **Low-Contrast Outlines** rather than aggressive shadows.

1.  **Level 0 (Background):** Neutral light gray (#F8F9FA). No shadow.
2.  **Level 1 (Cards/Tables):** White (#FFFFFF) with a 1px solid border (#DEE2E6). This is the primary surface for data.
3.  **Level 2 (Dropdowns/Modals):** White surfaces with a soft, diffused shadow (0px 4px 12px rgba(0,0,0,0.08)) to indicate temporary overlay.
4.  **Active State:** The active navigation item uses a colored background tint (#E8F1FF) and a vertical 4px primary-colored bar on the leading edge to show focus without using depth.

## Shapes

The shape language is **Soft (0.25rem)**. This provides a professional look that is slightly more modern and approachable than sharp corners.

- **Standard Elements:** Input fields, buttons, and filter containers use a 4px (0.25rem) radius.
- **Large Elements:** Main content cards and data table containers use an 8px (0.5rem) radius.
- **Pill Elements:** Status badges and pagination buttons use a fully rounded (100px) radius to distinguish them from structural UI components.

## Components

- **Side Navigation:** Items feature a 20px icon, 14px label, and a 48px height. Active states use the `secondary_color` background and `primary_color` text.
- **Data Tables:** Headers are `label-md` with a subtle gray background. Rows use 1px bottom borders. Hover states should trigger a #F1F3F5 background tint.
- **Status Badges:** Compact labels with high-radius corners. Use low-saturation background tints (e.g., light yellow) with high-contrast text for accessibility.
- **Input Fields:** Search bars use a subtle 1px border and a leading icon. Height is 40px for filters and 44px for global search.
- **Pagination:** Active page is represented by a solid `primary_color` square with rounded corners (4px).
- **Expansion Detail:** Within tables, use a light-blue tinted sub-section for "AI Extraction" details to visually group them with the parent record.