import type { ReactNode } from 'react'
import { useNavigate } from 'react-router-dom'

const adminAssets = {
  profile: 'https://www.figma.com/api/mcp/asset/ab7f7651-2873-4e7c-a4a6-bee97ae487b2.png',
  search: 'https://www.figma.com/api/mcp/asset/9ab8c171-740e-4111-a01c-c724ce224ce2.svg',
  bell: 'https://www.figma.com/api/mcp/asset/f248b48d-bf09-4c6c-9a4b-8ae32b98d156.svg',
  settings: 'https://www.figma.com/api/mcp/asset/ae26e8fa-bc5c-4e32-b993-abba882f4783.svg',
  dashboard: 'https://www.figma.com/api/mcp/asset/d1585f2d-b662-4e8b-9349-d1f252e536c3.svg',
  review: 'https://www.figma.com/api/mcp/asset/cfc42e52-df2a-4587-8e4f-a540decfbca3.svg',
  places: 'https://www.figma.com/api/mcp/asset/10150450-26aa-4064-8c31-04828f29601c.svg',
  routes: 'https://www.figma.com/api/mcp/asset/48ed457e-319c-4eb3-8454-f10ac1701770.svg',
  knowledge: 'https://www.figma.com/api/mcp/asset/e9a22f21-044f-4466-88d3-e9314a2b20f0.svg',
} as const

export type AdminSection = 'dashboard' | 'review' | 'places' | 'routes' | 'knowledge'

type Props = {
  active: AdminSection
  children: ReactNode
  searchPlaceholder?: string
  profileMode?: 'photo' | 'initials'
  topbarTitle?: string
}

const navItems: Array<{ key: AdminSection; label: string; icon: string; path: string }> = [
  { key: 'dashboard', label: '대시보드', icon: adminAssets.dashboard, path: '/admin' },
  { key: 'review', label: '검수 대기', icon: adminAssets.review, path: '/admin/reviews' },
  { key: 'places', label: '장소 관리', icon: adminAssets.places, path: '/admin/places' },
  { key: 'routes', label: '경로 관리', icon: adminAssets.routes, path: '/admin/routes' },
  { key: 'knowledge', label: '운영 지식 목록', icon: adminAssets.knowledge, path: '/admin/routes/knowledge' },
]

export function AdminShell({ active, children, searchPlaceholder = '검색...', profileMode = 'photo', topbarTitle }: Props) {
  const navigate = useNavigate()

  return (
    <div className="admin-shell">
      <aside className="admin-sidenav">
        <div className="admin-brand">
          <strong>Logistics Pro<br />Admin</strong>
          <span>Operations Control</span>
        </div>
        <nav className="admin-nav" aria-label="관리자 메뉴">
          {navItems.map((item) => (
            <button
              key={item.key}
              className={`admin-nav__item ${active === item.key ? 'is-active' : ''}`}
              type="button"
              onClick={() => navigate(item.path)}
            >
              <img src={item.icon} alt="" />
              <span>{item.label}</span>
            </button>
          ))}
        </nav>
      </aside>

      <header className="admin-topbar">
        {topbarTitle ? <h1 className="admin-topbar__title">{topbarTitle}</h1> : <label className="admin-topbar__search">
          <img src={adminAssets.search} alt="" />
          <input type="search" placeholder={searchPlaceholder} />
        </label>}
        <div className="admin-topbar__actions">
          <button className="admin-icon-action admin-icon-action--notify" type="button" aria-label="알림">
            <img src={adminAssets.bell} alt="" />
          </button>
          <button className="admin-icon-action" type="button" aria-label="설정">
            <img src={adminAssets.settings} alt="" />
          </button>
          <span className="admin-topbar__divider" />
          {profileMode === 'initials' ? (
            <div className="admin-profile-initials">AD</div>
          ) : (
            <button className="admin-profile-photo" type="button" aria-label="관리자 프로필">
              <img src={adminAssets.profile} alt="" />
            </button>
          )}
          {profileMode === 'initials' && <span className="admin-profile-label">Administrator Profile</span>}
        </div>
      </header>

      <main className="admin-main">{children}</main>
    </div>
  )
}
