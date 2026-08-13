import { useEffect, useState } from 'react'
import { useLocation, useNavigate, useSearchParams } from 'react-router-dom'
import { api } from '../api/client'
import type { GuidanceSession } from '../api/models'
import { routes } from '../routes'
import { guidanceSessionStore } from '../state/guidanceSession'

const mapImage = 'https://www.figma.com/api/mcp/asset/5eec0064-266f-4b42-8218-18917f25670d.png'
const menuIcon = 'https://www.figma.com/api/mcp/asset/cc18dc51-59cc-4082-b5de-2bc9465521f9.svg'
const locationIcon = 'https://www.figma.com/api/mcp/asset/691c7a28-c7c8-49a8-8948-91a7c062add6.svg'
const plusIcon = 'https://www.figma.com/api/mcp/asset/2f0d402a-aa86-42d7-bdc7-b8d01866f6db.svg'
const minusIcon = 'https://www.figma.com/api/mcp/asset/7c013ee9-d6ef-4be6-a20f-90108753093f.svg'
const turnIcon = 'https://www.figma.com/api/mcp/asset/6c291f76-5bd6-4303-9577-7cd424704072.svg'
const endIcon = 'https://www.figma.com/api/mcp/asset/3325c95e-6f0a-4d56-aecc-265a77415cc0.svg'

export function GuidanceStepPage() {
  const navigate = useNavigate()
  const location = useLocation()
  const [searchParams] = useSearchParams()
  const initialSession = (location.state as { session?: GuidanceSession } | null)?.session ?? null
  const [session, setSession] = useState<GuidanceSession | null>(initialSession)
  const [loading, setLoading] = useState(!initialSession)
  const [error, setError] = useState('')
  const sessionId = Number(searchParams.get('sessionId') ?? guidanceSessionStore.read()?.sessionId)

  useEffect(() => {
    if (session || !Number.isFinite(sessionId)) {
      if (!Number.isFinite(sessionId)) setError('진행 중인 안내 세션이 없습니다.')
      setLoading(false)
      return
    }
    let active = true
    api.guidance(sessionId)
      .then((response) => { if (active) setSession(response) })
      .catch((reason) => { if (active) setError(reason instanceof Error ? reason.message : '안내를 불러오지 못했습니다.') })
      .finally(() => { if (active) setLoading(false) })
    return () => { active = false }
  }, [session, sessionId])

  const advance = async () => {
    if (!session || loading) return
    setLoading(true)
    setError('')
    try {
      if (session.currentStep.isLastStep) {
        await api.completeGuidance(session.sessionId)
        guidanceSessionStore.clear()
        navigate(routes.guidanceCompleted, { state: { session } })
      } else {
        setSession(await api.nextGuidance(session.sessionId))
      }
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : '다음 안내를 불러오지 못했습니다.')
    } finally {
      setLoading(false)
    }
  }

  const step = session?.currentStep

  return (
    <div className="mobile-page guidance-step-page" data-figma-node="118:9178">
      <header className="guidance-header guidance-header--plain">
        <button type="button" aria-label="이전 화면" onClick={() => navigate(routes.home)}>
          <img src={menuIcon} alt="" />
        </button>
      </header>

      <div className="guidance-step-map">
        <img src={mapImage} alt="단계별 길안내 지도" />
      </div>

      <aside className="guidance-map-controls" aria-label="지도 컨트롤">
        <button type="button" aria-label="현재 위치">
          <img src={locationIcon} alt="" />
        </button>
        <div>
          <button type="button" aria-label="확대"><img src={plusIcon} alt="" /></button>
          <button type="button" aria-label="축소"><img src={minusIcon} alt="" /></button>
        </div>
      </aside>

      <section className="guidance-step-card" aria-label="현재 안내 단계">
        <div className="guidance-step-card__head">
          <img src={turnIcon} alt="" />
          <strong>{step ? `${step.sequenceNo}/${step.totalSteps} · ${step.toNodeName}` : '안내 준비 중'}</strong>
        </div>
        <div className="guidance-step-card__body">
          <div className="guidance-step-card__meta">
            <strong>{step?.movementMode ?? '-'}</strong>
            <span className="guidance-step-card__divider" aria-hidden="true" />
            <strong>{step?.traversalMethod ?? '-'}</strong>
          </div>
          <p>{step?.instruction ?? (error || '현재 단계를 불러오고 있습니다.')}</p>
          {!!step?.cards.length && (
            <div className="guidance-knowledge-list">
              {step.cards.map((card) => (
                <article className={`guidance-knowledge guidance-knowledge--${card.kind.toLowerCase()}`} key={card.knowledgeId}>
                  <div><strong>{card.kind}</strong>{card.isRecentlyAdded && <em>새 팁</em>}</div>
                  <p>{card.actionText ?? card.statement}</p>
                  {(card.conditionLabel || card.targetName) && <small>{[card.targetName, card.conditionLabel].filter(Boolean).join(' · ')}</small>}
                </article>
              ))}
            </div>
          )}
          {error && <p className="guidance-api-error" role="alert">{error}</p>}
        </div>
      </section>

      <button className="guidance-end" type="button" disabled={!session || loading} onClick={advance}>
        <img src={endIcon} alt="" />
        <span>{loading ? '불러오는 중...' : step?.isLastStep ? '배송 완료' : '다음 단계'}</span>
      </button>
    </div>
  )
}
