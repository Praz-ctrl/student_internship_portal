package com.portal.service;

import com.portal.entity.*;
import com.portal.exception.ApiException;
import com.portal.repository.OfferRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class OfferService {

    private final OfferRepository offerRepository;
    private final ApplicationService applicationService;
    private final NotificationService notificationService;
    private final AuditService auditService;

    public Offer extend(AppUser companyUser, Long applicationId) {
        InternshipApplication app = applicationService.getForCompany(companyUser, applicationId);
        if (app.getStatus() != ApplicationStatus.SELECTED) {
            throw new ApiException("Only selected candidates can receive an offer", HttpStatus.BAD_REQUEST);
        }
        if (offerRepository.findByApplication(app).isPresent()) {
            throw new ApiException("An offer has already been extended to this candidate", HttpStatus.CONFLICT);
        }
        Offer offer = offerRepository.save(Offer.builder().application(app).build());
        notificationService.notify(app.getStudent().getAppUser(), "OFFER",
                "Offer letter: " + app.getInternship().getCompany().getCompanyName()
                        + " has offered you the " + app.getInternship().getRole() + " internship");

        auditService.logEvent(companyUser.getId(), companyUser.getEmail(), "COMPANY",
                "EXTEND_OFFER", "OFFER", offer.getId(),
                "Extended offer to " + app.getStudent().getName() + " for " + app.getInternship().getRole(), null);

        return offer;
    }

    @Transactional(readOnly = true)
    public List<Offer> forStudent(Student student) {
        return offerRepository.findByApplicationStudent(student);
    }

    public Offer respond(Student student, Long offerId, OfferStatus status) {
        if (status != OfferStatus.ACCEPTED && status != OfferStatus.DECLINED) {
            throw new ApiException("Status must be ACCEPTED or DECLINED", HttpStatus.BAD_REQUEST);
        }
        Offer offer = offerRepository.findById(offerId)
                .orElseThrow(() -> new ApiException("Offer not found", HttpStatus.NOT_FOUND));
        if (!offer.getApplication().getStudent().getId().equals(student.getId())) {
            throw new ApiException("This offer is not yours", HttpStatus.FORBIDDEN);
        }
        if (offer.getStatus() != OfferStatus.EXTENDED) {
            throw new ApiException("You have already responded to this offer", HttpStatus.CONFLICT);
        }
        offer.setStatus(status);
        offerRepository.save(offer);
        notificationService.notify(offer.getApplication().getInternship().getCompany().getAppUser(), "OFFER",
                student.getName() + " " + status.name().toLowerCase() + " the offer for "
                        + offer.getApplication().getInternship().getRole());

        auditService.logEvent(student.getAppUser().getId(), student.getAppUser().getEmail(), "STUDENT",
                "RESPOND_OFFER_" + status.name(), "OFFER", offerId,
                "Student " + status.name().toLowerCase() + " the offer for " + offer.getApplication().getInternship().getRole(), null);

        return offer;
    }
}
