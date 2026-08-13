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

export interface PlaceNode {
  id: number
  nodeCode: string
  nodeType: string
  name: string
  floorLabel: string | null
  isIndoor: boolean
}

export interface PlaceDetail {
  id: number
  name: string
  placeType: string
  description: string | null
  nodes: PlaceNode[]
  routes: Array<{
    id: number
    routeCode: string
    name: string
    isDefault: boolean
    destinationNodeId: number
  }>
}

export interface KnowledgeTarget {
  target_type: 'NODE' | 'SEGMENT' | 'PLACE' | 'UNKNOWN'
  target_code: string | null
  target_resolution_status: 'RESOLVED' | 'UNRESOLVED'
  target_free_text: string | null
}

export interface KnowledgeConditions {
  vehicle_class: string | null
  min_tonnage: number | null
  max_tonnage: number | null
  max_vehicle_height_m: number | null
  max_vehicle_width_m: number | null
  active_time_start: string | null
  active_time_end: string | null
  active_days: string[] | null
  extra_condition_text: string | null
}

export interface KnowledgePayload {
  target: KnowledgeTarget
  category: string
  custom_category_label: string | null
  fact_type: string
  custom_fact_type_label: string | null
  movement_mode: string
  traversal_method: string | null
  custom_traversal_method: string | null
  access_state: string | null
  statement: string
  action_text: string | null
  source_excerpt: string
  conditions: KnowledgeConditions
  usage_scope: string
}

export interface ModerationDraftSummary {
  draftId: number
  reportId: number
  placeName: string
  createdAt: string
  summary: string
}

export interface ModerationDraftDetail {
  draftId: number
  status: string
  report: {
    id: number
    audioUrl: string | null
    rawSttText: string | null
    correctedSttText: string | null
    placeName: string
    scopeNodeName: string | null
  }
  payload: KnowledgePayload
  resolvedTargetName: string | null
}

export interface ReportCreated {
  reportId: number
  rawSttText: string
}

export interface ReportExtraction {
  reportId: number
  status: 'EXTRACTED' | 'EXTRACTION_FAILED'
  drafts: Array<{ draftId: number; draftIndex: number; payload: KnowledgePayload }> | null
  reason: string | null
}

export interface ApiErrorBody {
  error: { code: string; message: string }
}
