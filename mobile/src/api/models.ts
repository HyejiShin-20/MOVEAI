export type DeliveryJobStatus = 'READY' | 'IN_PROGRESS' | 'DONE' | string
export type KnowledgeCardKind = 'WARNING' | 'ACTION' | 'REFERENCE'

export interface PlaceSummary {
  id: number
  placeCode: string
  name: string
  placeType: string
  description: string | null
}

export interface DeliveryJobSummary {
  id: number
  jobCode: string
  placeId: number
  placeName: string
  recipientLabel: string
  addressText: string
  itemSummary: string
  status: DeliveryJobStatus
}

export interface DeliveryJobDetail {
  id: number
  jobCode: string
  place: { id: number; name: string }
  destinationNode: { id: number; name: string }
  recipientLabel: string
  addressText: string
  itemSummary: string
  status: DeliveryJobStatus
}

export interface VehicleInput {
  vehicleClass: 'TRUCK' | string
  tonnage: number
  heightM: number
  widthM: number | null
}

export interface GuidanceCard {
  knowledgeId: number
  kind: KnowledgeCardKind
  statement: string
  actionText: string | null
  conditionLabel: string | null
  isRecentlyAdded: boolean
  targetName: string | null
  isUnresolvedTarget: boolean
}

export interface GuidanceStep {
  sequenceNo: number
  totalSteps: number
  fromNodeName: string
  toNodeName: string
  movementMode: string
  traversalMethod: string
  instruction: string
  isLastStep: boolean
  cards: GuidanceCard[]
}

export interface GuidanceSession {
  sessionId: number
  route: { id: number; name: string; totalSteps: number }
  currentStep: GuidanceStep
}

export interface ApiErrorBody {
  error: { code: string; message: string }
}
