// particles.js — Animated particle background (Lunar Client style)
export function initParticles() {
    const canvas = document.getElementById('particles-canvas');
    if (!canvas) return;

    const ctx = canvas.getContext('2d');
    let particles = [];
    let animId;
    let mouse = { x: -1000, y: -1000 };

    const CONFIG = {
        count: 60,
        color: '139, 92, 246', // Default accent
        maxSpeed: 0.4,
        size: 2,
        linkDistance: 150,
        linkOpacity: 0.15,
        mouseRadius: 200
    };

    window.setVIPParticles = function(isVIP) {
        if (isVIP) {
            CONFIG.color = '88, 101, 242'; // Blurple
            CONFIG.count = 100;
            CONFIG.size = 2.5;
            CONFIG.linkOpacity = 0.25;
            CONFIG.maxSpeed = 0.6;
        } else {
            CONFIG.color = '139, 92, 246';
            CONFIG.count = 60;
            CONFIG.size = 2;
            CONFIG.linkOpacity = 0.15;
            CONFIG.maxSpeed = 0.4;
        }
        init(); // Reinitialize with new count
    };

    function resize() {
        canvas.width = window.innerWidth;
        canvas.height = window.innerHeight;
    }

    function createParticle() {
        return {
            x: Math.random() * canvas.width,
            y: Math.random() * canvas.height,
            vx: (Math.random() - 0.5) * CONFIG.maxSpeed,
            vy: (Math.random() - 0.5) * CONFIG.maxSpeed,
            size: Math.random() * CONFIG.size + 0.5,
            opacity: Math.random() * 0.5 + 0.2
        };
    }

    function init() {
        resize();
        particles = Array.from({ length: CONFIG.count }, createParticle);
    }

    function drawParticle(p) {
        ctx.beginPath();
        ctx.arc(p.x, p.y, p.size, 0, Math.PI * 2);
        ctx.fillStyle = `rgba(${CONFIG.color}, ${p.opacity})`;
        ctx.fill();
    }

    function drawLinks() {
        for (let i = 0; i < particles.length; i++) {
            for (let j = i + 1; j < particles.length; j++) {
                const dx = particles[i].x - particles[j].x;
                const dy = particles[i].y - particles[j].y;
                const dist = Math.sqrt(dx * dx + dy * dy);

                if (dist < CONFIG.linkDistance) {
                    const opacity = (1 - dist / CONFIG.linkDistance) * CONFIG.linkOpacity;
                    ctx.beginPath();
                    ctx.moveTo(particles[i].x, particles[i].y);
                    ctx.lineTo(particles[j].x, particles[j].y);
                    ctx.strokeStyle = `rgba(${CONFIG.color}, ${opacity})`;
                    ctx.lineWidth = 0.5;
                    ctx.stroke();
                }
            }
        }
    }

    function update() {
        particles.forEach(p => {
            p.x += p.vx;
            p.y += p.vy;

            // Mouse repulsion
            const dx = p.x - mouse.x;
            const dy = p.y - mouse.y;
            const dist = Math.sqrt(dx * dx + dy * dy);
            if (dist < CONFIG.mouseRadius && dist > 0) {
                const force = (CONFIG.mouseRadius - dist) / CONFIG.mouseRadius * 0.02;
                p.vx += (dx / dist) * force;
                p.vy += (dy / dist) * force;
            }

            // Speed limit
            const speed = Math.sqrt(p.vx * p.vx + p.vy * p.vy);
            if (speed > CONFIG.maxSpeed * 2) {
                p.vx = (p.vx / speed) * CONFIG.maxSpeed;
                p.vy = (p.vy / speed) * CONFIG.maxSpeed;
            }

            // Wrap around edges
            if (p.x < -10) p.x = canvas.width + 10;
            if (p.x > canvas.width + 10) p.x = -10;
            if (p.y < -10) p.y = canvas.height + 10;
            if (p.y > canvas.height + 10) p.y = -10;
        });
    }

    function animate() {
        if (!CONFIG.active) return;
        ctx.clearRect(0, 0, canvas.width, canvas.height);
        update();
        drawLinks();
        particles.forEach(drawParticle);
        animId = requestAnimationFrame(animate);
    }

    CONFIG.active = true;
    window.toggleParticles = function(active) {
        if (CONFIG.active === active) return;
        CONFIG.active = active;
        if (active) {
            animate();
        } else {
            if (animId) cancelAnimationFrame(animId);
            ctx.clearRect(0, 0, canvas.width, canvas.height);
        }
    };

    // Events
    window.addEventListener('resize', () => {
        resize();
        if (!CONFIG.active) ctx.clearRect(0, 0, canvas.width, canvas.height);
    });

    document.addEventListener('mousemove', (e) => {
        if (!CONFIG.active) return;
        mouse.x = e.clientX;
        mouse.y = e.clientY;
    });

    document.addEventListener('mouseleave', () => {
        mouse.x = -1000;
        mouse.y = -1000;
    });

    init();
    animate();
}
