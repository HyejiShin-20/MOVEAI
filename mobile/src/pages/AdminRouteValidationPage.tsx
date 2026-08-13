import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { RouteAdminShell } from '../components/RouteAdminShell'

const assets = {
  modify: 'https://www.figma.com/api/mcp/asset/62f31a1d-e113-4c22-b13e-7f74e53f1c55.svg',
  publish: 'https://www.figma.com/api/mcp/asset/547fe448-c288-4fa0-aff7-2b8ed1179c79.svg',
  conditions: 'https://www.figma.com/api/mcp/asset/acda294e-121b-4114-8cff-c44f19797710.svg',
  run: 'https://www.figma.com/api/mcp/asset/45195c5f-ed93-41b2-bdb2-58800dc984b1.svg',
  phone: 'https://www.figma.com/api/mcp/asset/8ac84ab9-b108-4c57-9695-6df352891c8c.svg',
  direction: 'https://www.figma.com/api/mcp/asset/dfbfae62-0503-46c5-b18e-602e6752b781.svg',
  ai: 'https://www.figma.com/api/mcp/asset/91eff54a-31dd-4bf8-8ace-5dca775f1348.svg',
  photo: 'https://www.figma.com/api/mcp/asset/f40714b9-526f-4527-865f-dbd8cc110694.png',
  log: 'https://www.figma.com/api/mcp/asset/91a976e0-0284-4e49-a810-255eab3d214f.svg',
  check: 'https://www.figma.com/api/mcp/asset/b64e50a4-8cbf-431a-9f15-525be98c83d1.svg',
  reject: 'https://www.figma.com/api/mcp/asset/9403f31f-d871-4a11-b270-0c4dfb426618.svg',
  add: 'https://www.figma.com/api/mcp/asset/f6d54a48-876d-4a69-a72a-97484dac8d1c.svg',
} as const

export function AdminRouteValidationPage() {
  const navigate = useNavigate()
  const [vehicle, setVehicle] = useState('1.5t Box Truck')
  const [destination, setDestination] = useState('Building 101 - Loading Dock')
  const [time, setTime] = useState('10:00 AM (Standard)')
  const [ran, setRan] = useState(false)
  const [published, setPublished] = useState(false)

  return (
    <RouteAdminShell breadcrumb={['Routes', 'R-1042: Shinjuku Hub', 'Preview & Validation']} showSearch={false} profileMode="initials">
      <div className="route-validation-page">
        <header className="route-validation-heading"><div><h1>Route Validation <span>DRAFT MODE</span></h1><p>Simulate delivery conditions to verify AI-injected guidance and driver instructions.</p></div><div><button type="button" onClick={() => navigate('/admin/routes')}><img src={assets.modify} alt="" />Modify Route</button><button className="primary" type="button" onClick={() => setPublished(true)}><img src={assets.publish} alt="" />Publish Route</button></div></header>
        <section className="simulation-bar"><div className="simulation-label"><img src={assets.conditions} alt="" />Simulation Conditions:</div><label><span>Vehicle Profile</span><select value={vehicle} onChange={(e) => setVehicle(e.target.value)}><option>1.5t Box Truck</option><option>1t Van</option><option>4t Truck</option></select></label><label><span>Destination Target</span><select value={destination} onChange={(e) => setDestination(e.target.value)}><option>Building 101 - Loading Dock</option><option>Main Gate</option></select></label><label><span>Estimated Arrival</span><select value={time} onChange={(e) => setTime(e.target.value)}><option>10:00 AM (Standard)</option><option>8:00 PM (Night)</option></select></label><button type="button" onClick={() => setRan(true)}><img src={assets.run} alt="" />Run Simulation</button></section>
        {(ran || published) && <p className="validation-flash">{published ? 'Route published.' : `Simulation completed for ${vehicle} / ${time}.`}</p>}
        <div className="route-validation-grid">
          <section className="driver-simulation-card"><header><div><img src={assets.phone} alt="" /><h2>Driver Device Simulation</h2></div><span>Interactive Preview</span></header><div className="driver-simulation-map"><div className="driver-location-card"><img src={assets.direction} alt="" /><span><strong>In 200m, turn right</strong><small>onto Chuo-dori</small></span></div><i /></div><div className="turn-guide"><article><span>↑</span><div><h3>Continue straight</h3><p>on Harumi-dori for 1.2km</p></div></article><article className="ai-step"><span>↵</span><div><h3>Turn left</h3><p>onto Building 101 Access Road</p><section><h4><img src={assets.ai} alt="" />1.5t Truck Protocol</h4><p>Clearance is 2.8m. Proceed to underground loading bay B. Present ID at the security intercom before descending.</p><img src={assets.photo} alt="지하 하역장 사진" /></section></div></article><article><span>⚑</span><div><h3>Arrive at destination</h3><p>{destination}</p></div></article></div></section>
          <aside className="validation-log-card"><header><img src={assets.log} alt="" /><h2>Validation Log</h2></header><div className="validation-log-scroll"><h3><img src={assets.check} alt="" />Included Guidance</h3><section><strong>Height Clearance Warning</strong><p>Warns driver of 2.8m limit at entrance.</p><span>✓ Match: 1.5t Truck</span><span>✓ Match: Bldg 101 Dock</span></section><section><strong>Morning Dock Procedures</strong><p>Requires ID presentation before entry.</p><span>✓ Match: Time (10:00 AM)</span></section><h3><img src={assets.reject} alt="" />Excluded Guidance (Filtered)</h3><section className="excluded"><strong>Heavy Freight Routing</strong><span>× Reject: Vehicle != 4t+</span></section><section className="excluded"><strong>Night Access Code</strong><span>× Reject: Time != Post 20:00</span></section></div><footer><button type="button"><img src={assets.add} alt="" />Add Missing Knowledge</button></footer></aside>
        </div>
      </div>
    </RouteAdminShell>
  )
}
