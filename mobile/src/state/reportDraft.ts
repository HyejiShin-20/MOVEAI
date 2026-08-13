export type SelectedPlace = {
  id: number
  name: string
  address: string
  distance?: string
}

export type ReportDraftStage = 'recording' | 'uploading' | 'transcription' | 'place' | 'confirm'

export type ReportDraftState = {
  transcript: string
  selectedPlace: SelectedPlace | null
  selectedScopeNodeId: number | null
  selectedZone: string | null
  stage: ReportDraftStage
  updatedAt: string
}

const STORAGE_KEY = 'moveai.reportDraft.v1'

const emptyDraft = (): ReportDraftState => ({
  transcript: '',
  selectedPlace: null,
  selectedScopeNodeId: null,
  selectedZone: null,
  stage: 'recording',
  updatedAt: new Date().toISOString(),
})

function canUseStorage() {
  return typeof window !== 'undefined' && typeof window.localStorage !== 'undefined'
}

export const reportDraftStore = {
  get(): ReportDraftState {
    if (!canUseStorage()) return emptyDraft()

    const raw = window.localStorage.getItem(STORAGE_KEY)
    if (!raw) return emptyDraft()

    try {
      return { ...emptyDraft(), ...(JSON.parse(raw) as Partial<ReportDraftState>) }
    } catch {
      return emptyDraft()
    }
  },

  patch(patch: Partial<Omit<ReportDraftState, 'updatedAt'>>): ReportDraftState {
    const next: ReportDraftState = {
      ...this.get(),
      ...patch,
      updatedAt: new Date().toISOString(),
    }
    if (canUseStorage()) window.localStorage.setItem(STORAGE_KEY, JSON.stringify(next))
    return next
  },

  reset(): ReportDraftState {
    const next = emptyDraft()
    if (canUseStorage()) window.localStorage.setItem(STORAGE_KEY, JSON.stringify(next))
    return next
  },

  clear() {
    if (canUseStorage()) window.localStorage.removeItem(STORAGE_KEY)
  },
}
