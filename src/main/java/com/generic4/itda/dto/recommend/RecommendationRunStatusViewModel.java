package com.generic4.itda.dto.recommend;

import com.generic4.itda.domain.recommendation.constant.RecommendationRunStatus;

/**
 * 추천 실행(run) 상태를 기반으로 결과 페이지에서 사용자에게 노출할 데이터를 담는 ViewModel.
 *
 * <p>이 ViewModel은 RecommendationRun 도메인 객체를 그대로 노출하지 않고,
 * 화면에서 필요한 정보(문구, 상태, 다음 액션)를 가공하여 전달하기 위한 역할을 가진다.</p>
 *
 * <p>특히 비동기 추천 실행 흐름에서 결과가 아직 생성되지 않은 경우에도
 * 상태(PENDING / RUNNING / COMPUTED / FAILED)에 따라 사용자에게 적절한 안내 메시지와 후속 액션을 제공하기 위해 사용된다.</p>
 *
 * <h3>설계 의도</h3>
 * <ul>
 *     <li>도메인 상태를 UI 친화적인 형태로 변환한다.</li>
 *     <li>템플릿에서 상태 분기 로직을 최소화한다.</li>
 *     <li>추천 결과 페이지의 "상태 셸(shell)" 역할을 지원한다.</li>
 * </ul>
 *
 * <h3>상태별 동작 예시</h3>
 * <ul>
 *     <li>PENDING / RUNNING: 진행 중 안내 및 새로고침 유도</li>
 *     <li>COMPUTED: 결과 목록 페이지로 이동 가능</li>
 *     <li>FAILED: 실패 안내 및 재시도 유도</li>
 * </ul>
 *
 * @param proposalId      제안서 식별자 (URL 및 링크 생성에 사용)
 * @param runId           추천 실행 식별자 (URL 및 상태 조회에 사용)
 * @param proposalTitle   제안서 제목 (사용자 컨텍스트 표시용)
 * @param status          추천 실행 상태 (도메인 상태)
 * @param title           상태에 따른 주요 안내 제목
 * @param message         상태에 따른 상세 안내 메시지
 * @param nextActionLabel 사용자에게 제공할 다음 액션 버튼 라벨
 * @param nextActionUrl   다음 액션으로 이동할 URL (없을 경우 null 가능)
 * @param autoRefresh     자동 새로고침 여부 (진행 중 상태에서 true)
 */
public record RecommendationRunStatusViewModel(
        Long proposalId,
        Long runId,
        String proposalTitle,
        RecommendationRunStatus status,
        String title,
        String message,
        String nextActionLabel,
        String nextActionUrl,
        boolean autoRefresh
) {

}