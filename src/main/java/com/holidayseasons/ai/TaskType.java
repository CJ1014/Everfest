package com.holidayseasons.ai;

/** Kinds of holiday activity a villager can be given. Which ones a holiday uses comes from its {@code behaviors}. */
public enum TaskType {
    /** Carry a decoration to a spot and put it up (preparation). */
    DECORATE("decorate", false, true),
    /** Walk to the festival square and mingle/dance there. */
    GATHER("gather", false, false),
    /** Walk over to another villager and chat. */
    SOCIALIZE("socialize", false, false),
    /** Share a meal at the festival square. */
    FEAST("feast", false, false),
    /** Bring a present to another villager. */
    GIFT_EXCHANGE("gift_exchange", false, false),
    /** Launch fireworks at the festival square. */
    FIREWORKS("fireworks", true, false),
    /** Take a decoration down during cleanup. */
    TIDY_UP("tidy_up", false, false),
    /** Babies: knock on doors for candy. */
    TRICK_OR_TREAT("trick_or_treat", false, false),
    /** Adults: stand by the door / stall handing out candy. */
    HAND_OUT_CANDY("hand_out_candy", true, false),
    /** Babies: look for hidden eggs. */
    EGG_HUNT("egg_hunt", false, false),
    /** Adults: hide eggs before Easter. */
    HIDE_EGGS("hide_eggs", true, true),
    /** Visit a registered player home. */
    VISIT_HOME("visit_homes", false, false);

    private final String behavior;
    private final boolean adultsOnly;
    private final boolean preparation;

    TaskType(String behavior, boolean adultsOnly, boolean preparation) {
        this.behavior = behavior;
        this.adultsOnly = adultsOnly;
        this.preparation = preparation;
    }

    /** The holiday {@code behaviors} id that enables this task. */
    public String behavior() {
        return behavior;
    }

    public boolean adultsOnly() {
        return adultsOnly;
    }

    public boolean isPreparation() {
        return preparation;
    }
}
