import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { api } from '../api/client'
import type { PlaceNode, PlaceSummary } from '../api/models'
import { routes } from '../routes'
import { reportDraftStore } from '../state/reportDraft'

const mapImage = 'https://www.figma.com/api/mcp/asset/6acbc0da-7822-49de-844f-71a63603f417.png'
const menuIcon = 'https://www.figma.com/api/mcp/asset/bf2948ce-d989-4f2b-b8ec-a274f5862119.svg'
const profileIcon = 'https://www.figma.com/api/mcp/asset/e5224a83-059b-4d41-b981-041ae7257bcf.svg'
const myLocationIcon = 'https://www.figma.com/api/mcp/asset/0063424b-1488-49bd-960d-0788eaaca3b5.svg'
const markerIcon = 'https://www.figma.com/api/mcp/asset/d0236cc9-9ca3-46df-8c9d-1bd04f300bc1.svg'
const selectedIcon = 'https://www.figma.com/api/mcp/asset/06af1398-09de-4327-a298-e6ec739537da.svg'
const completeIcon = 'https://www.figma.com/api/mcp/asset/e83f3727-6274-4935-ba08-89bc53231f53.svg'
const searchIcon = 'https://www.figma.com/api/mcp/asset/99bf81dd-7dda-4617-a744-04fd0797c69c.svg'

export function PlaceSelectPage() {
  const navigate = useNavigate()
  const savedDraft = reportDraftStore.get()
  const [places, setPlaces] = useState<PlaceSummary[]>([])
  const [nodes, setNodes] = useState<PlaceNode[]>([])
  const [selectedPlace, setSelectedPlace] = useState<number | null>(savedDraft.selectedPlace?.id ?? null)
  const [selectedNode, setSelectedNode] = useState<number | null>(savedDraft.selectedScopeNodeId)
  const [query, setQuery] = useState('')
  const [error, setError] = useState('')

  useEffect(() => {
    let active = true
    api.places().then((response) => {
      if (!active) return
      setPlaces(response)
      setSelectedPlace((current) => current ?? response.find((place) => place.placeCode === 'PLACE_B')?.id ?? response[0]?.id ?? null)
    }).catch((reason) => { if (active) setError(reason instanceof Error ? reason.message : '장소를 불러오지 못했습니다.') })
    return () => { active = false }
  }, [])

  useEffect(() => {
    if (!selectedPlace) return
    let active = true
    api.place(selectedPlace).then((response) => {
      if (!active) return
      setNodes(response.nodes)
      setSelectedNode((current) => response.nodes.some((node) => node.id === current) ? current : null)
    }).catch((reason) => { if (active) setError(reason instanceof Error ? reason.message : '내부 위치를 불러오지 못했습니다.') })
    return () => { active = false }
  }, [selectedPlace])

  const filteredPlaces = places.filter((place) => !query.trim() || place.name.toLocaleLowerCase().includes(query.trim().toLocaleLowerCase()))
  const selectedPlaceData = places.find((place) => place.id === selectedPlace)
  const selectedNodeData = nodes.find((node) => node.id === selectedNode)

  const complete = () => {
    if (!selectedPlaceData) return
    reportDraftStore.patch({
      selectedPlace: { id: selectedPlaceData.id, name: selectedPlaceData.name, address: selectedPlaceData.description ?? selectedPlaceData.placeType },
      selectedScopeNodeId: selectedNode,
      selectedZone: selectedNodeData?.name ?? null,
      stage: 'confirm',
    })
    navigate(routes.reportConfirm)
  }

  return (
    <div className="mobile-page place-select-page">
      <header className="place-header">
        <button type="button" aria-label="메뉴" onClick={() => navigate(routes.reportDrafts)}><img src={menuIcon} alt="" /></button>
        <strong>Logistics Pro</strong>
        <button type="button" aria-label="프로필" onClick={() => navigate(routes.myReports)}><img src={profileIcon} alt="" /></button>
      </header>
      <main className="place-main">
        <section className="place-search">
          <h1>방금 배송을 완료한 장소를 지정해 주세요</h1>
          <label><img src={searchIcon} alt="" /><input type="search" placeholder="등록 장소 검색" value={query} onChange={(event) => setQuery(event.target.value)} /></label>
          {error && <p className="place-api-error" role="alert">{error}</p>}
        </section>
        <section className="place-map" aria-label="현재 위치 지도">
          <img className="place-map__image" src={mapImage} alt="배송지 주변 지도 시안" />
          <div className="place-current-marker" aria-hidden="true"><span>현재 위치</span><img src={markerIcon} alt="" /></div>
          <button className="place-location-button" type="button" aria-label="내 위치"><img src={myLocationIcon} alt="" /></button>
        </section>
        <section className="place-sheet">
          <div className="place-sheet__handle" aria-hidden="true"><span /></div><h2>등록된 장소</h2>
          <div className="place-list">
            {filteredPlaces.map((place, index) => {
              const selected = selectedPlace === place.id
              return (
                <article key={place.id} className={selected ? 'place-card place-card--selected' : 'place-card'} onClick={() => { setSelectedPlace(place.id); setSelectedNode(null) }}>
                  <div className="place-card__top">
                    <span className="place-card__number">{index + 1}</span>
                    <div className="place-card__copy"><h3>{place.name}</h3><p>{place.description ?? place.placeType}</p></div>
                    {selected && <img className="place-card__selected-icon" src={selectedIcon} alt="선택됨" />}
                  </div>
                  {selected && <div className="place-zones"><span>대표 내부 위치 선택 (선택)</span><div>{nodes.map((node) => (
                    <button key={node.id} type="button" className={selectedNode === node.id ? 'active' : ''} onClick={(event) => { event.stopPropagation(); setSelectedNode(node.id) }}>
                      {node.floorLabel ? `${node.floorLabel} · ` : ''}{node.name}
                    </button>
                  ))}</div></div>}
                </article>
              )
            })}
            {!filteredPlaces.length && <p className="place-empty">검색 결과가 없습니다.</p>}
            <div className="place-list__bottom-space" />
          </div>
          <div className="place-complete-wrap"><button className="place-complete" type="button" disabled={!selectedPlaceData} onClick={complete}><img src={completeIcon} alt="" /><span>장소 선택 완료</span></button></div>
        </section>
      </main>
    </div>
  )
}
