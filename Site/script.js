const root = document.documentElement;
const tabButtons = Array.from(document.querySelectorAll(".tab-button"));
const tabPanels = Array.from(document.querySelectorAll(".showcase-panel"));
const showcaseSection = document.querySelector("#showcase");
const revealElements = document.querySelectorAll("[data-reveal]");
const navLinks = Array.from(document.querySelectorAll(".site-nav a"));
const sections = Array.from(document.querySelectorAll("main section[id]"));
const parallaxTarget = document.querySelector(".hero-parallax");
const prefersReducedMotion = window.matchMedia("(prefers-reduced-motion: reduce)").matches;

let activeTab = "home";
let rotationTimer = null;
let autoRotatePaused = false;

function activateTab(tabId) {
  const nextPanel = tabPanels.find((panel) => panel.dataset.panel === tabId);
  if (!nextPanel) return;

  activeTab = tabId;
  const accent = nextPanel.dataset.accent || "#ff4fa0";
  root.style.setProperty("--tab-accent", accent);

  tabButtons.forEach((button) => {
    const isActive = button.dataset.tab === tabId;
    button.classList.toggle("is-active", isActive);
    button.setAttribute("aria-selected", String(isActive));
  });

  tabPanels.forEach((panel) => {
    panel.classList.toggle("is-active", panel.dataset.panel === tabId);
  });
}

function nextTab() {
  const currentIndex = tabButtons.findIndex((button) => button.dataset.tab === activeTab);
  const nextIndex = (currentIndex + 1) % tabButtons.length;
  activateTab(tabButtons[nextIndex].dataset.tab);
}

function startAutoRotate() {
  if (prefersReducedMotion) return;
  stopAutoRotate();
  rotationTimer = window.setInterval(() => {
    if (autoRotatePaused) return;
    nextTab();
  }, 5500);
}

function stopAutoRotate() {
  if (rotationTimer) {
    window.clearInterval(rotationTimer);
    rotationTimer = null;
  }
}

tabButtons.forEach((button) => {
  button.addEventListener("click", () => {
    activateTab(button.dataset.tab);
    autoRotatePaused = true;
  });
});

if (showcaseSection) {
  showcaseSection.addEventListener("mouseenter", () => {
    autoRotatePaused = true;
  });

  showcaseSection.addEventListener("mouseleave", () => {
    autoRotatePaused = false;
  });
}

const revealObserver = new IntersectionObserver(
  (entries) => {
    entries.forEach((entry) => {
      if (entry.isIntersecting) {
        entry.target.classList.add("is-visible");
        revealObserver.unobserve(entry.target);
      }
    });
  },
  {
    threshold: 0.18,
    rootMargin: "0px 0px -8% 0px",
  }
);

revealElements.forEach((element) => {
  if (prefersReducedMotion) {
    element.classList.add("is-visible");
  } else {
    revealObserver.observe(element);
  }
});

const sectionObserver = new IntersectionObserver(
  (entries) => {
    entries.forEach((entry) => {
      if (!entry.isIntersecting) return;
      navLinks.forEach((link) => {
        const isCurrent = link.getAttribute("href") === `#${entry.target.id}`;
        link.classList.toggle("is-current", isCurrent);
      });
    });
  },
  {
    threshold: 0.55,
  }
);

sections.forEach((section) => sectionObserver.observe(section));

if (parallaxTarget && !prefersReducedMotion) {
  window.addEventListener("mousemove", (event) => {
    const xRatio = (event.clientX / window.innerWidth - 0.5) * 2;
    const yRatio = (event.clientY / window.innerHeight - 0.5) * 2;
    const xMove = xRatio * 12;
    const yMove = yRatio * 10;
    parallaxTarget.style.transform = `perspective(1400px) rotateY(${xMove * 0.36}deg) rotateX(${yMove * -0.28}deg) translate3d(${xMove}px, ${yMove}px, 0)`;
  });

  window.addEventListener("mouseleave", () => {
    parallaxTarget.style.transform = "";
  });
}

activateTab(activeTab);
startAutoRotate();
