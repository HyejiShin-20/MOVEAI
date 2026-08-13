const STORAGE_KEY = 'moveai.guidance-session'

export interface SavedGuidanceSession {
  deliveryJobId: number
  sessionId: number
}

export const guidanceSessionStore = {
  read(): SavedGuidanceSession | null {
    const value = sessionStorage.getItem(STORAGE_KEY)
    if (!value) return null

    try {
      const parsed = JSON.parse(value) as Partial<SavedGuidanceSession>
      if (typeof parsed.deliveryJobId !== 'number' || typeof parsed.sessionId !== 'number') {
        return null
      }
      return { deliveryJobId: parsed.deliveryJobId, sessionId: parsed.sessionId }
    } catch {
      return null
    }
  },

  save(value: SavedGuidanceSession) {
    sessionStorage.setItem(STORAGE_KEY, JSON.stringify(value))
  },

  clear() {
    sessionStorage.removeItem(STORAGE_KEY)
  },
}
