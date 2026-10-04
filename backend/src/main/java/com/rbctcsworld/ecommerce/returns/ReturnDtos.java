package com.rbctcsworld.ecommerce.returns;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public final class ReturnDtos {

    private ReturnDtos() {
    }

    public record CreateReturnRequest(
            @NotBlank @Pattern(regexp = "DAMAGED|WRONG_ITEM|NOT_AS_DESCRIBED|NO_LONGER_NEEDED",
                    message = "must be DAMAGED, WRONG_ITEM, NOT_AS_DESCRIBED or NO_LONGER_NEEDED") String reason,
            @Size(max = 500) String comment) {
    }

    public record ResolveReturnRequest(@Size(max = 255) String note) {
    }
}
