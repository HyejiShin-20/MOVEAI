import { useMemo, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { RouteAdminShell } from '../components/RouteAdminShell'

const assets = {
  route: 'https://www.figma.com/api/mcp/asset/cdcc5453-ae7c-4839-b13f-bd0cfecccdfe.svg',
  view: 'https://www.figma.com/api/mcp/asset/593813a7-4fb5-4575-919b-20c0567957bc.svg',
  save: 'https://www.figma.com/api/mcp/asset/a53f933c-dc17-4d0b-acff-bcecfbc5cf52.svg',
  node: 'https://www.figma.com/api/mcp/asset/e2b4af90-5f18-490e-bf22-3e7b98884a11.svg',
  link: 'https://www.figma.com/api/mcp/asset/c3df41b5-50e8-4afb-ae96-0d2092f7ae07.svg',
  selected: 'https://www.figma.com/api/mcp/asset/a4f800cd-a930-4623-8d8c-997c1a4199fa.svg',
  linked: 'https://www.figma.com/api/mcp/asset/29fb421f-6311-4b9f-9b73-716a7647bc8b.svg',
  vehicle: 'https://www.figma.com/api/mcp/asset/0499c5e0-642d-4efd-a4c5-7d5fa77c23a3.svg',
  access: 'https://www.figma.com/api/mcp/asset/df58a544-97e1-495c-b43e-639a7766c5cd.svg',
  driver: 'https://www.figma.com/api/mcp/asset/d6122941-23df-47a9-8be3-1d3754042e5c.svg',
} as const

const routeNodes = [
  ['Start Point', 'Main Entrance', 0],
  ['Back Gate', 'Secondary Access Point', 2],
  ['Loading Dock B', 'Heavy Freight Area', 1],
] as const

export function AdminRouteKnowledgePage() {
  const navigate = useNavigate()
  const [selectedIndex, setSelectedIndex] = useState(1)
  const [linked, setLinked] = useState(['Vehicle Restriction', 'Access Requirement'])
  const current = routeNodes[selectedIndex]
  const previews = useMemo(() => linked, [linked])

  return (
    <RouteAdminShell breadcrumb={['Admin Service', 'Route Management', 'Knowledge Connection']}>
      <div className="route-knowledge-page">
        <header className="route-knowledge-heading"><div><h1>Route: Industrial Park Alpha</h1><p>Manage knowledge connections for nodes and segments along this route.</p></div><div><button type="button" onClick={() => navigate('/admin/routes')}><img src={assets.view} alt="" />View Full Route</button><button className="primary" type="button" onClick={() => navigate('/admin/routes/validation')}><img src={assets.save} alt="" />Save Connections</button></div></header>
        <div className="route-knowledge-grid">
          <section className="route-knowledge-left">
            <article className="route-knowledge-map-card"><header><h2>Interactive Route Map</h2><div><button type="button">⌕</button><button type="button">⌕</button></div></header><div className="route-knowledge-map"><img src={assets.route} alt="경로 오버레이" /><button className="map-node one" type="button" onClick={() => setSelectedIndex(0)} /><button className={`map-node two ${selectedIndex === 1 ? 'is-selected' : ''}`} type="button" onClick={() => setSelectedIndex(1)}><img src={assets.node} alt="" /></button><button className="map-node three" type="button" onClick={() => setSelectedIndex(2)} /></div></article>
            <article className="route-node-list"><header><h2>Route Segments &amp; Nodes</h2></header>{routeNodes.map(([name, subtitle, count], index) => <button type="button" className={selectedIndex === index ? 'is-active' : ''} onClick={() => setSelectedIndex(index)} key={name}><span>{index + 1}</span><span><strong>{name}</strong><small>{subtitle}</small></span><em>{count} Linked</em></button>)}</article>
          </section>
          <aside className="route-knowledge-right">
            <article className="route-selected-card"><div><small>SELECTED NODE</small><h2>{current[0]}</h2></div><img src={assets.selected} alt="" /><footer><button className="primary" type="button" onClick={() => setLinked((items) => items.includes('New Site Note') ? items : [...items, 'New Site Note'])}>↪ Link New</button><button type="button">⌕ Search</button></footer></article>
            <article className="linked-knowledge-card"><header><img src={assets.linked} alt="" /><h2>Linked Knowledge ({linked.length})</h2></header><div className="linked-knowledge-list">
              {linked.map((name, index) => <section className={index === 1 ? 'critical' : ''} key={name}><h3><img src={index === 1 ? assets.access : assets.vehicle} alt="" />{name}</h3><p>{name === 'Vehicle Restriction' ? '1.5t vehicle accessible only. Larger trucks cannot clear overhead piping.' : name === 'Access Requirement' ? <>Gate code required: <code>#4492</code>. Must call dispatch if code fails.</> : 'Temporary operational note linked by admin.'}</p><footer><span>{index === 0 ? 'AI · Extracted from Driver Logs' : 'Added by Admin'}</span><em>{index === 1 ? 'Critical' : 'High Confidence'}</em></footer></section>)}
            </div></article>
            <article className="driver-preview-card"><span>DRIVER APP PREVIEW</span><header><img src={assets.driver} alt="" /><div><small>Arriving at</small><strong>{current[0]}</strong></div></header>{previews.slice(0,2).map((name, index) => <div className={index ? 'warning' : ''} key={name}><strong>{index ? 'Gate Code Required' : 'Vehicle Limit'}</strong><small>{index ? 'Use code #4492' : '1.5t vehicle accessible only.'}</small></div>)}</article>
          </aside>
        </div>
      </div>
    </RouteAdminShell>
  )
}
