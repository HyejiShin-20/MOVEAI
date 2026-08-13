import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { RouteAdminShell } from '../components/RouteAdminShell'

const assets = {
  map: 'https://www.figma.com/api/mcp/asset/62037005-24b7-49e7-9811-3c7530c3cb9b.png',
  plus: 'https://www.figma.com/api/mcp/asset/a682e9c5-169c-4725-8ccb-5ac6803f6480.svg',
  minus: 'https://www.figma.com/api/mcp/asset/4838a2ad-7ef2-4405-a67a-8ac1213947fb.svg',
  locate: 'https://www.figma.com/api/mcp/asset/ad88f385-bf0b-488e-a8df-df25738a0979.svg',
  vehicle: 'https://www.figma.com/api/mcp/asset/7b12d83c-9aaf-47b6-b291-8bbc8e17e5d9.svg',
  foot: 'https://www.figma.com/api/mcp/asset/2da72c79-a06c-43c1-b21b-3ed16f2a021f.svg',
  gate: 'https://www.figma.com/api/mcp/asset/86171bd2-3fde-491c-a9b9-30d63e93b3aa.svg',
  parking: 'https://www.figma.com/api/mcp/asset/d3d337f7-956a-443b-b851-6acd75852f79.svg',
  building: 'https://www.figma.com/api/mcp/asset/647be7e0-64c4-4116-aeb2-fff13b88d6cf.svg',
  elevator: 'https://www.figma.com/api/mcp/asset/e191cbf5-2d5c-4fff-97d7-5337794e7b2a.svg',
  warning: 'https://www.figma.com/api/mcp/asset/d5bf057b-85e4-4c42-99b8-09295802427c.svg',
  add: 'https://www.figma.com/api/mcp/asset/d6b5a71f-4603-4588-8bf7-ef919527ecf2.svg',
  save: 'https://www.figma.com/api/mcp/asset/ac863afa-666d-435a-b74b-49171df3f7b6.svg',
} as const

type RouteNode = { name: string; note?: string; kind: 'gate' | 'parking' | 'building' | 'elevator'; state?: 'active' | 'transition' | 'danger' }

const initialNodes: RouteNode[] = [
  { name: 'Main Gate', note: 'Security check required.', kind: 'gate', state: 'active' },
  { name: 'Back Gate (Vehicle)', kind: 'gate' },
  { name: 'Parking Area A', note: 'Switch to foot traffic here.', kind: 'parking', state: 'transition' },
  { name: 'Building 101 Entrance', kind: 'building' },
  { name: 'Freight Elevator', note: 'AI Note: Often out of service.', kind: 'elevator', state: 'danger' },
]

export function AdminRouteEditPage() {
  const navigate = useNavigate()
  const [mode, setMode] = useState<'vehicle' | 'foot'>('vehicle')
  const [nodes, setNodes] = useState(initialNodes)
  const [saved, setSaved] = useState(false)

  const icons = { gate: assets.gate, parking: assets.parking, building: assets.building, elevator: assets.elevator }

  return (
    <RouteAdminShell breadcrumb={['Admin Service', 'Route Management', 'Edit Route: HQ Delivery']} searchPlaceholder="Search locations, routes...">
      <div className="route-edit-layout">
        <section className="route-edit-map">
          <img src={assets.map} alt="HQ Delivery Path 지도" />
          <span className="route-edit-status"><i />Route Active - Editing Mode</span>
          <div className="route-edit-map-controls"><button type="button">＋</button><button type="button">−</button><button type="button">◎</button></div>
        </section>
        <aside className="route-edit-panel">
          <header><div><h1>HQ Delivery Path</h1><p>Last 100m Routing Sequence</p></div><span>DRAFT</span></header>
          <div className="route-mode-toggle"><button className={mode === 'vehicle' ? 'is-active' : ''} type="button" onClick={() => setMode('vehicle')}><img src={assets.vehicle} alt="" />Vehicle</button><button className={mode === 'foot' ? 'is-active' : ''} type="button" onClick={() => setMode('foot')}><img src={assets.foot} alt="" />Foot</button></div>
          <div className="route-sequence">
            {nodes.map((node, index) => <article className={`route-sequence-item ${node.state ? `is-${node.state}` : ''}`} key={`${node.name}-${index}`}><span className="route-sequence-index">{index + 1}</span><div><header><span><img src={icons[node.kind]} alt="" />{node.name}</span>{node.state === 'transition' && <em>Transition</em>}</header>{node.note && <p className={node.state === 'danger' ? 'is-danger' : ''}>{node.state === 'danger' && <img src={assets.warning} alt="" />}{node.note}</p>}</div></article>)}
            <button className="route-add-node" type="button" onClick={() => setNodes((current) => [...current, { name: `New Node ${current.length + 1}`, kind: 'building' }])}><img src={assets.add} alt="" />Add Node</button>
          </div>
          <footer><button className="primary" type="button" onClick={() => { setSaved(true); navigate('/admin/routes/knowledge') }}><img src={assets.save} alt="" />Save Route</button><button type="button" onClick={() => setNodes(initialNodes)}>Discard Changes</button>{saved && <small>Route saved.</small>}</footer>
        </aside>
      </div>
    </RouteAdminShell>
  )
}
