from typing import Literal

from pydantic import BaseModel, ConfigDict, Field, model_validator


class HealthResponse(BaseModel):
    status: str
    provider: str
    model: str


class SttResponse(BaseModel):
    text: str
    durationMs: int


class StrictModel(BaseModel):
    model_config = ConfigDict(extra="forbid")


class KnownLocation(StrictModel):
    code: str = Field(min_length=1, max_length=32)
    name: str = Field(min_length=1, max_length=120)


class KnowledgeExtractionRequest(StrictModel):
    placeName: str = Field(min_length=1, max_length=100)
    transcript: str = Field(min_length=1)
    scopeNodeName: str | None = Field(default=None, max_length=100)
    knownNodes: list[KnownLocation]
    knownSegments: list[KnownLocation]

    @model_validator(mode="after")
    def ensure_unique_context_codes(self) -> "KnowledgeExtractionRequest":
        node_codes = [node.code for node in self.knownNodes]
        segment_codes = [segment.code for segment in self.knownSegments]
        if len(node_codes) != len(set(node_codes)):
            raise ValueError("knownNodes.code는 중복될 수 없습니다.")
        if len(segment_codes) != len(set(segment_codes)):
            raise ValueError("knownSegments.code는 중복될 수 없습니다.")
        return self


TargetType = Literal["PLACE", "NODE", "SEGMENT", "UNKNOWN"]
TargetResolutionStatus = Literal["RESOLVED", "UNRESOLVED", "NEEDS_REVIEW"]
Category = Literal[
    "ACCESS",
    "PARKING_STOPPING",
    "LOADING",
    "BUILDING_ENTRANCE",
    "INTERNAL_ROUTE",
    "ELEVATOR_STAIRS",
    "CONGESTION_WAIT",
    "DELIVERY_POINT",
    "OTHER",
]
FactType = Literal[
    "RESTRICTION",
    "ALLOWANCE",
    "LOCATION",
    "INSTRUCTION",
    "WARNING",
    "CONDITION",
    "OTHER",
]
MovementMode = Literal["VEHICLE", "PEDESTRIAN", "GENERAL"]
TraversalMethod = Literal["DRIVE", "WALK", "STAIRS", "ELEVATOR", "ESCALATOR", "CART", "OTHER"]
AccessState = Literal["ALLOWED", "CONDITIONAL", "PROHIBITED", "UNKNOWN"]
UsageScope = Literal["WARNING_ONLY", "ACTION_GUIDANCE", "ROUTE_GUIDANCE", "REFERENCE_ONLY"]
DayOfWeek = Literal["MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN"]


class KnowledgeTarget(StrictModel):
    target_type: TargetType
    target_code: str | None
    target_resolution_status: TargetResolutionStatus
    target_free_text: str | None


class KnowledgeConditions(StrictModel):
    vehicle_class: Literal["TRUCK"] | None
    min_tonnage: float | None = Field(ge=0)
    max_tonnage: float | None = Field(ge=0)
    max_vehicle_height_m: float | None = Field(ge=0)
    max_vehicle_width_m: float | None = Field(ge=0)
    active_time_start: str | None = Field(pattern=r"^(?:[01]\d|2[0-3]):[0-5]\d$")
    active_time_end: str | None = Field(pattern=r"^(?:[01]\d|2[0-3]):[0-5]\d$")
    active_days: list[DayOfWeek] | None
    extra_condition_text: str | None


class ExtractedKnowledgeItem(StrictModel):
    target: KnowledgeTarget
    category: Category
    custom_category_label: str | None
    fact_type: FactType
    custom_fact_type_label: str | None
    movement_mode: MovementMode
    traversal_method: TraversalMethod | None
    custom_traversal_method: str | None
    access_state: AccessState | None
    statement: str = Field(min_length=1, max_length=500)
    action_text: str | None = Field(max_length=300)
    source_excerpt: str = Field(min_length=1, max_length=500)
    conditions: KnowledgeConditions
    usage_scope: UsageScope


class KnowledgeExtractionResponse(StrictModel):
    items: list[ExtractedKnowledgeItem] = Field(max_length=20)
