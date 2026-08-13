import { useEffect, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { api } from '../api/client'
import type { KnowledgePayload, ModerationDraftDetail } from '../api/models'
import { AdminShell } from '../components/AdminShell'
import { routes } from '../routes'

const assets = {
  source: 'https://www.figma.com/api/mcp/asset/e23a37c3-1f09-45ce-818d-48cfc9585f4e.svg', pin: 'https://www.figma.com/api/mcp/asset/1bce6de9-ee0e-4081-ad80-302051fdb46f.svg', transcript: 'https://www.figma.com/api/mcp/asset/e8b93c35-2f45-4e6a-a0c5-78494fb9b707.svg', ai: 'https://www.figma.com/api/mcp/asset/7ea20510-7198-42df-8806-3b402d77ef42.svg', count: 'https://www.figma.com/api/mcp/asset/f3213649-12c3-4c8e-bc97-f8b51e55cf56.svg', reject: 'https://www.figma.com/api/mcp/asset/4302f05a-1227-4cbd-b330-b8f317785efc.svg', edit: 'https://www.figma.com/api/mcp/asset/d68d782b-d6da-4600-a2f3-41e651e67bd2.svg', approve: 'https://www.figma.com/api/mcp/asset/3d4f7eea-8f54-473d-93da-adfd0900c452.svg', map: 'https://www.figma.com/api/mcp/asset/fd0075e7-3e22-4138-a2d4-910d961c8dd0.png',
} as const

export function AdminReviewDetailPage() {
  const navigate = useNavigate()
  const { reportId } = useParams()
  const draftId = Number(reportId)
  const [detail, setDetail] = useState<ModerationDraftDetail | null>(null)
  const [payload, setPayload] = useState<KnowledgePayload | null>(null)
  const [editing, setEditing] = useState(false)
  const [processing, setProcessing] = useState(false)
  const [result, setResult] = useState('')
  const [error, setError] = useState('')

  useEffect(() => {
    if (!Number.isFinite(draftId)) { setError('올바르지 않은 초안 번호입니다.'); return }
    let active = true
    api.moderationDraft(draftId).then((response) => { if (active) { setDetail(response); setPayload(response.payload) } }).catch((reason) => { if (active) setError(reason instanceof Error ? reason.message : '초안을 불러오지 못했습니다.') })
    return () => { active = false }
  }, [draftId])

  const updatePayload = (field: 'statement' | 'action_text', value: string) => setPayload((current) => !current ? current : field === 'statement' ? { ...current, statement: value } : { ...current, action_text: value || null })
  const approve = async () => {
    if (!payload || processing) return
    setProcessing(true); setError('')
    try { const response = await api.approveDraft(draftId, editing ? payload : undefined); setResult(`승인 완료 · 지식 #${response.knowledgeId} · 임베딩 생성됨`); setDetail((current) => current ? { ...current, status: 'APPROVED' } : current); setEditing(false) }
    catch (reason) { setError(reason instanceof Error ? reason.message : '승인하지 못했습니다.') }
    finally { setProcessing(false) }
  }
  const reject = async () => {
    if (processing) return
    const reason = window.prompt('반려 사유를 입력하세요.')?.trim()
    if (!reason) return
    setProcessing(true); setError('')
    try { await api.rejectDraft(draftId, reason); setResult('반려 완료'); setDetail((current) => current ? { ...current, status: 'REJECTED' } : current) }
    catch (cause) { setError(cause instanceof Error ? cause.message : '반려하지 못했습니다.') }
    finally { setProcessing(false) }
  }

  const transcript = detail?.report.correctedSttText || detail?.report.rawSttText
  const targetLabel = detail?.resolvedTargetName || payload?.target.target_free_text || payload?.target.target_code || '확인 필요'
  const pending = detail?.status === 'PENDING'

  return (
    <AdminShell active="review" profileMode="initials">
      <div className="admin-review-detail-page">
        <header className="admin-review-detail-heading"><div><div className="admin-review-detail-meta"><span>DRAFT-{detail?.draftId ?? reportId}</span><em>{detail?.status ?? '불러오는 중'}</em></div><h1>제보 상세 검수</h1><p>원문과 AI 추출 결과를 대조한 뒤 승인하거나 반려합니다.</p></div><div className="admin-review-detail-actions"><button type="button" onClick={() => navigate(routes.adminReviews)}>목록으로</button></div></header>
        {error && !detail && <p className="admin-detail-state is-error" role="alert">{error}</p>}
        {detail && payload && <div className="admin-review-detail-grid">
          <section className="admin-source-panel"><h2><img src={assets.source} alt="" />원본 제보 데이터</h2><div className="admin-source-location"><img src={assets.pin} alt="" /><div><strong>{detail.report.placeName}</strong><span>{detail.report.scopeNodeName ?? '대표 위치 미지정'}</span></div></div><div className="admin-source-map"><img src={assets.map} alt="현장 지도 시안" /></div><div className="admin-audio-box"><div><strong>제보 녹음본</strong><span>{detail.report.audioUrl ? '파일 저장됨' : '텍스트 제보'}</span></div></div><div className="admin-transcript"><h3><img src={assets.transcript} alt="" />검수 기준 원문</h3><p>{transcript ?? '원문이 없습니다.'}</p></div></section>
          <section className="admin-ai-panel"><div className="admin-ai-heading"><div><img src={assets.ai} alt="" /><h2>AI 추출 지식</h2><span><img src={assets.count} alt="" />초안 1건</span></div></div><article className={`admin-knowledge-card ${detail.status === 'APPROVED' ? 'is-approved' : detail.status === 'REJECTED' ? 'is-rejected' : ''}`}>
            <div className="admin-knowledge-card__head"><div><span>{payload.fact_type}</span><em>{payload.custom_category_label ?? payload.category}</em></div><b><i />{payload.usage_scope}</b></div>
            <div className="admin-knowledge-grid"><label><span>안내 문장</span><textarea readOnly={!editing} value={payload.statement} onChange={(event) => updatePayload('statement', event.target.value)} /></label><label><span>행동 안내</span><textarea readOnly={!editing} value={payload.action_text ?? ''} onChange={(event) => updatePayload('action_text', event.target.value)} placeholder="행동 안내 없음" /></label><label><span>이동 방식 / 대상</span><div className="admin-static-field">{payload.movement_mode} / {targetLabel}{payload.target.target_type === 'UNKNOWN' ? ' (확인 필요)' : ''}</div></label><label><span>조건</span><div className="admin-static-field">{payload.conditions.extra_condition_text ?? '별도 조건 없음'}</div></label><label className="span-2"><span>근거 원문</span><blockquote>{payload.source_excerpt}</blockquote></label></div>
            {pending && <div className="admin-knowledge-actions"><button className="reject" type="button" disabled={processing} onClick={reject}><img src={assets.reject} alt="" />반려</button><button type="button" disabled={processing} onClick={() => setEditing((value) => !value)}><img src={assets.edit} alt="" />{editing ? '수정 취소' : '수정'}</button><button className="approve" type="button" disabled={processing} onClick={approve}><img src={assets.approve} alt="" />{processing ? '임베딩 중...' : editing ? '수정 승인' : '승인'}</button></div>}
            {result && <span className="admin-knowledge-result">{result}</span>}{error && <span className="admin-knowledge-result is-error" role="alert">{error}</span>}
          </article></section>
        </div>}
      </div>
    </AdminShell>
  )
}
