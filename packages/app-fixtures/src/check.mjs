import {
  child,
  currentWish,
  dashboardMetrics,
  family,
  pendingReviews,
  roomState,
  todayTasks,
  weeklyPlan
} from "./index.js";

if (!family.id || !child.id || child.familyId !== family.id) {
  throw new Error("Family and child fixtures must be linked.");
}

if (todayTasks.length < 3) {
  throw new Error("Today fixture must cover multiple task states.");
}

if (currentWish.currentFragments > currentWish.targetFragments) {
  throw new Error("Wish progress cannot exceed the target.");
}

if (pendingReviews.length !== dashboardMetrics.pendingReviews) {
  throw new Error("Dashboard review metric must match review fixtures.");
}

if (!weeklyPlan.rules.every((rule) => Array.isArray(rule.weekdays) && rule.weekdays.length > 0)) {
  throw new Error("Weekly plan rules must include weekdays.");
}

if (!roomState.items.some((item) => item.unlocked === false)) {
  throw new Error("Room fixture must include locked and unlocked items.");
}

console.log(`validated fixtures for ${todayTasks.length} tasks and ${pendingReviews.length} reviews`);
