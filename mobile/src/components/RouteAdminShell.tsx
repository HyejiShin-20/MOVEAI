import type { ReactNode } from 'react'
import { useNavigate } from 'react-router-dom'

const shellAssets = {
  profile: 'https://www.figma.com/api/mcp/asset/5fd44cc5-d2b3-4fdf-9ab1-2852afb87d2f.png',
  search: 'https://www.figma.com/api/mcp/asset/77510396-f2fc-42de-b421-dbbb31e0f9da.svg',
  bell: 'https://www.figma.com/api/mcp/asset/b15f4384-d96c-46e4-a572-22de88d5fd40.svg',
  help: 'https://www.figma.com/api/mcp/asset/b92b29d5-78d7-400a-8d87-682938cda834.svg',
  plus: 'https://www.figma.com/api/mcp/asset/725d836d-f2cd-41b3-b369-7958af141c58.svg',
  dashboard: 'https://www.figma.com/api/mcp/asset/a682e9c5-169c-4725-8ccb-5ac6803f6480.svg',
  reviews: 'https://www.figma.com/api/mcp/asset/4838a2ad-7ef2-4405-a67a-8ac1213947fb.svg',
  locations: 'https://www.figma.com/api/mcp/asset/ad88f385-bf0b-488e-a8df-df25738a0979.svg',
  routes: 'https://www.figma.com/api/mcp/asset/9d6a12da-c82b-4f3d-978c-20dc8f26ed7b.svg',
  knowledge: 'https://www.figma.com/api/mcp/asset/1e129eb1-41a2-4bd7-86d3-b11c1abacb2f.svg',
  settings: 'https://www.figma.com/api/mcp/asset/1a9facdd-7f12-42bd-b08a-422ef0314c09.svg',
  support: 'https://www.figma.com/api/mcp/asset/0a67bbfd-10d5-4d64-b14d-70b06b89ad0f.svg',
} as const

type Props = {
  children: ReactNode
  breadcrumb: string[]
  showSearch?: boolean
  searchPlaceholder?: string
  profileMode?: 'photo' | 'initials'
}

const navItems = [
  { label: 'Dashboard', icon: shellAssets.dashboard, path: '/admin' },
  { label: 'Pending Reviews', icon: shellAssets.reviews, path: '/admin/reviews' },
  { label: 'Location Management', icon: shellAssets.locations, path: '/admin/places' },
  { label: 'Route Management', icon: shellAssets.routes, path: '/admin/routes', active: true },
  { label: 'Knowledge Library', icon: shellAssets.knowledge, path: '/admin/routes/knowledge' },
]

export function RouteAdminShell({ children, breadcrumb, showSearch = true, searchPlaceholder = 'Search routes...', profileMode = 'photo' }: Props) {
  const navigate = useNavigate()

  return (
    <div className="route-admin-shell">
      <aside className="route-admin-side">
        <div className="route-admin-brand">
          <span className="route-admin-brand__mark">LP</span>
          <span><strong>Logistics Pro</strong><small>Admin Console</small></span>
        </div>
        <button className="route-admin-new" type="button" onClick={() => navigate('/admin/routes')}><img src={shellAssets.plus} alt="" />New Route</button>
        <nav className="route-admin-nav" aria-label="Route admin navigation">
          {navItems.map((item) => (
            <button key={item.label} type="button" className={item.active ? 'is-active' : ''} onClick={() => navigate(item.path)}>
              <img src={item.icon} alt="" /><span>{item.label}</span>
            </button>
          ))}
        </nav>
        <div className="route-admin-footer-nav">
          <button type="button"><img src={shellAssets.settings} alt="" />Settings</button>
          <button type="button"><img src={shellAssets.support} alt="" />Support</button>
        </div>
      </aside>

      <header className="route-admin-top">
        <div className="route-admin-breadcrumb">
          {breadcrumb.map((part, index) => (
            <span key={`${part}-${index}`} className={index === breadcrumb.length - 1 ? 'is-current' : ''}>{part}{index < breadcrumb.length - 1 && <i>›</i>}</span>
          ))}
        </div>
        <div className="route-admin-top__actions">
          {showSearch && <label className="route-admin-search"><img src={shellAssets.search} alt="" /><input type="search" placeholder={searchPlaceholder} /></label>}
          <button type="button" className="route-admin-icon" aria-label="알림"><img src={shellAssets.bell} alt="" /></button>
          <button type="button" className="route-admin-icon" aria-label="도움말"><img src={shellAssets.help} alt="" /></button>
          {profileMode === 'photo' ? <span className="route-admin-profile"><img src={shellAssets.profile} alt="" /></span> : <span className="route-admin-profile route-admin-profile--initials">AD</span>}
        </div>
      </header>

      <main className="route-admin-main">{children}</main>
    </div>
  )
}
