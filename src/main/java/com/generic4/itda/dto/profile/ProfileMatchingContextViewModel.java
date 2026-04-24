package com.generic4.itda.dto.profile;

public record ProfileMatchingContextViewModel(
        Long matchingId,
        String viewerRole,
        String matchingStatus,
        boolean contactVisible,
        String statusLabel,
        String helperMessage,
        String matchingDetailUrl,
        String proposalDetailUrl,
        String acceptActionUrl,
        String rejectActionUrl,
        String contactEmail,
        String contactPhone
) {
    public boolean proposed() {
        return "PROPOSED".equals(matchingStatus);
    }

    public boolean freelancerViewer() {
        return "FREELANCER".equals(viewerRole);
    }

    public boolean canRespond() {
        return proposed() && freelancerViewer();
    }

    public boolean hasContactDetails() {
        return contactVisible
                && contactEmail != null
                && !contactEmail.isBlank()
                && contactPhone != null
                && !contactPhone.isBlank();
    }
}
