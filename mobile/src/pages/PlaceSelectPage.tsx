import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
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

const places = [
  { number: 1, name: '래미안 강남힐즈 101동', address: '서울 강남구 자곡로 130', distance: '12m' },
  { number: 2, name: 'CU 자곡점', address: '서울 강남구 자곡로 130 상가 1층', distance: '25m' },
  { number: 3, name: '세곡푸르지오 203동', address: '서울 강남구 자곡로 115', distance: '58m' },
]

const zones = ['101동 공동현관', '101동 후문', '경비실 앞', '지하주차장 A구역']

export function PlaceSelectPage() {
  const navigate = useNavigate()
  const savedDraft = reportDraftStore.get()
  const [selectedPlace, setSelectedPlace] = useState(savedDraft.selectedPlace?.id ?? 1)
  const [selectedZone, setSelectedZone] = useState(savedDraft.selectedZone ?? zones[0])

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
          <label>
            <img src={searchIcon} alt="" />
            <input type="search" placeholder="건물명, 도로명, 지번 검색" />
          </label>
        </section>

        <section className="place-map" aria-label="현재 위치 지도">
          <img className="place-map__image" src={mapImage} alt="배송지 주변 지도" />
          <div className="place-current-marker" aria-hidden="true">
            <span>현재 위치</span>
            <img src={markerIcon} alt="" />
          </div>
          <button className="place-location-button" type="button" aria-label="내 위치"><img src={myLocationIcon} alt="" /></button>
        </section>

        <section className="place-sheet">
          <div className="place-sheet__handle" aria-hidden="true"><span /></div>
          <h2>주변 장소 (100m 이내)</h2>
          <div className="place-list">
            {places.map((place) => {
              const selected = selectedPlace === place.number
              return (
                <article key={place.number} className={selected ? 'place-card place-card--selected' : 'place-card'} onClick={() => setSelectedPlace(place.number)}>
                  <div className="place-card__top">
                    <span className="place-card__number">{place.number}</span>
                    <div className="place-card__copy">
                      <h3>{place.name}</h3>
                      <p>{place.address}</p>
                      <span className={place.number === 1 ? 'place-distance place-distance--near' : 'place-distance'}>{place.distance}</span>
                    </div>
                    {selected && <img className="place-card__selected-icon" src={selectedIcon} alt="선택됨" />}
                  </div>
                  {selected && place.number === 1 && (
                    <div className="place-zones">
                      <span>내부 구역 선택</span>
                      <div>
                        {zones.map((zone) => (
                          <button key={zone} type="button" className={selectedZone === zone ? 'active' : ''} onClick={(event) => { event.stopPropagation(); setSelectedZone(zone) }}>
                            {zone}
                          </button>
                        ))}
                      </div>
                    </div>
                  )}
                </article>
              )
            })}
            <div className="place-list__bottom-space" />
          </div>
          <div className="place-complete-wrap">
            <button className="place-complete" type="button" onClick={() => { const place = places.find((item) => item.number === selectedPlace) ?? places[0]; reportDraftStore.patch({ selectedPlace: { id: place.number, name: place.name, address: place.address, distance: place.distance }, selectedZone, stage: 'confirm' }); navigate(routes.reportConfirm) }}>
              <img src={completeIcon} alt="" /><span>장소 선택 완료</span>
            </button>
          </div>
        </section>
      </main>
    </div>
  )
}
