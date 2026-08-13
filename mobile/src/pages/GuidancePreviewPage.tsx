import { useEffect, useState } from 'react'
import type { FormEvent } from 'react'
import { useNavigate, useSearchParams } from 'react-router-dom'
import { ApiError, api } from '../api/client'
import type { DeliveryJobDetail } from '../api/models'
import { routes } from '../routes'
import { guidanceSessionStore } from '../state/guidanceSession'

const mapImage = 'https://www.figma.com/api/mcp/asset/3b0a65b7-25b6-4ffc-a1eb-6a31f00ca4c5.png'
const stopPhoto = 'https://www.figma.com/api/mcp/asset/6a3cb7d3-559a-4699-a6c5-08baa2d1ddc5.png'
const backGatePhoto = 'https://www.figma.com/api/mcp/asset/ac1cc887-4fc3-4dac-9f06-a428907c62d4.png'
const menuIcon = 'https://www.figma.com/api/mcp/asset/482f2379-ef48-42a9-95b6-93591c4fc8d4.svg'
const startIcon = 'https://www.figma.com/api/mcp/asset/0fa96c88-41c3-44ff-8aa6-2ccb294c83a8.svg'

const photoCards = [
  { image: stopPhoto, label: '정차 위치' },
  { image: backGatePhoto, label: '후문 사진' },
  { image: backGatePhoto, label: '후문 사진' },
]

export function GuidancePreviewPage() {
  const navigate = useNavigate()
  const [searchParams] = useSearchParams()
  const [job, setJob] = useState<DeliveryJobDetail | null>(null)
  const [tonnage, setTonnage] = useState('1.0')
  const [heightM, setHeightM] = useState('2.3')
  const [widthM, setWidthM] = useState('')
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')

  useEffect(() => {
    let active = true
    const requestedId = Number(searchParams.get('jobId'))
    const savedId = guidanceSessionStore.read()?.deliveryJobId

    const load = async () => {
      try {
        let jobId = Number.isFinite(requestedId) && requestedId > 0 ? requestedId : savedId
        if (!jobId) {
          const jobs = await api.deliveryJobs()
          jobId = jobs.find((candidate) => candidate.jobCode === 'JOB_B_01')?.id ?? jobs[0]?.id
        }
        if (!jobId) throw new Error('안내할 배송 건이 없습니다.')
        const detail = await api.deliveryJob(jobId)
        if (active) setJob(detail)
      } catch (reason) {
        if (active) setError(reason instanceof Error ? reason.message : '배송 정보를 불러오지 못했습니다.')
      }
    }
    void load()
    return () => { active = false }
  }, [searchParams])

  const startGuidance = async (event: FormEvent) => {
    event.preventDefault()
    if (!job || loading) return
    setLoading(true)
    setError('')

    try {
      const session = await api.startGuidance(job.id, {
        vehicleClass: 'TRUCK',
        tonnage: Number(tonnage),
        heightM: Number(heightM),
        widthM: widthM ? Number(widthM) : null,
      })
      guidanceSessionStore.save({ deliveryJobId: job.id, sessionId: session.sessionId })
      navigate(`${routes.guidanceStep}?sessionId=${session.sessionId}`, { state: { session } })
    } catch (reason) {
      if (reason instanceof ApiError && reason.code === 'NO_ROUTE_AVAILABLE') {
        navigate(routes.guidanceUnavailable, {
          state: { jobId: job.id, message: reason.message, tonnage, heightM, widthM },
        })
        return
      }
      setError(reason instanceof Error ? reason.message : '안내를 시작하지 못했습니다.')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="mobile-page guidance-preview-page" data-figma-node="118:9145">
      <header className="guidance-header guidance-header--plain">
        <button type="button" aria-label="이전 화면" onClick={() => navigate(routes.home)}>
          <img src={menuIcon} alt="" />
        </button>
      </header>

      <main className="guidance-map-wrap">
        <img className="guidance-map" src={mapImage} alt="Last 100m 경로 미리보기 지도" />

        <form id="guidance-start-form" className="guidance-setup-card" onSubmit={startGuidance}>
          <div>
            <strong>{job?.place.name ?? '배송 정보를 불러오는 중입니다.'}</strong>
            <span>{job ? `${job.recipientLabel} · ${job.itemSummary}` : ''}</span>
          </div>
          <div className="guidance-vehicle-fields">
            <label><span>톤수(t)</span><input type="number" min="0.1" step="0.1" required value={tonnage} onChange={(event) => setTonnage(event.target.value)} /></label>
            <label><span>높이(m)</span><input type="number" min="0.1" step="0.01" required value={heightM} onChange={(event) => setHeightM(event.target.value)} /></label>
            <label><span>너비(m)</span><input type="number" min="0.1" step="0.01" placeholder="선택" value={widthM} onChange={(event) => setWidthM(event.target.value)} /></label>
          </div>
          {error && <p role="alert">{error}</p>}
        </form>

        <div className="guidance-photo-strip" aria-label="경로 참고 사진">
          {photoCards.map((card, index) => (
            <article className="guidance-photo-card" key={`${card.label}-${index}`}>
              <img src={card.image} alt="" />
              <span>{card.label}</span>
            </article>
          ))}
        </div>

        <div className="guidance-start-wrap">
          <button type="submit" form="guidance-start-form" disabled={!job || loading}>
            <img src={startIcon} alt="" />
            <span>{loading ? '경로를 확인하는 중...' : '안내 시작'}</span>
          </button>
        </div>
      </main>
    </div>
  )
}
