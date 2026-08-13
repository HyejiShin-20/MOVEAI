import { useLocation, useNavigate } from 'react-router-dom'
import { publicAsset } from '../publicAsset'
import { routes } from '../routes'

const items = [
  { label: '배송', icon: publicAsset('nav-delivery.svg'), path: routes.home, matches: ['/home'] },
  { label: '현장 팁', icon: publicAsset('nav-tip.svg'), path: routes.reportRecord, matches: ['/reports'] },
  { label: '경로안내', icon: publicAsset('nav-route.svg'), path: routes.guidancePreview, matches: ['/guidance'] },
  { label: 'MY', icon: publicAsset('nav-my.svg'), path: routes.myReports, matches: ['/reports/mine'], compact: true },
]

export function BottomNav() {
  const navigate = useNavigate()
  const location = useLocation()

  return (
    <nav className="bottom-nav" aria-label="주요 메뉴">
      {items.map((item) => {
        const active = item.label === 'MY'
          ? location.pathname === routes.myReports
          : item.matches.some((prefix) => location.pathname.startsWith(prefix)) && location.pathname !== routes.myReports

        return (
          <button
            className={`bottom-nav__item${active ? ' is-active' : ''}`}
            type="button"
            key={item.label}
            aria-current={active ? 'page' : undefined}
            onClick={() => navigate(item.path)}
          >
            <span className={`bottom-nav__icon${item.compact ? ' bottom-nav__icon--compact' : ''}`}>
              <img src={item.icon} alt="" />
            </span>
            <span>{item.label}</span>
          </button>
        )
      })}
    </nav>
  )
}
