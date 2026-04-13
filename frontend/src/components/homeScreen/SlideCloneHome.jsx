import React from "react";
import "./SlideCloneHome.css";

import topRightBadge from "../../assests/slideClone/root_8_x10800081_y0.png";
import logoMain from "../../assests/slideClone/root_9_0_x436122_y1022432.png";
import logoTagline from "../../assests/slideClone/root_9_1_x703385_y1682946.png";
import partnerGraphic from "../../assests/slideClone/root_6_0_x8074138_y1299691.png";
import heroBanner from "../../assests/slideClone/root_3_x5332543_y2451457.png";
import cardTop from "../../assests/slideClone/root_21_x2686924_y2597444.jpg";
import cardBottom from "../../assests/slideClone/root_2_x2687265_y3898761.jpg";
import cardLeftBottom from "../../assests/slideClone/root_18_x1592718_y4269982.png";
import cardFarLeft from "../../assests/slideClone/root_16_x251492_y2936185.png";
import cardMapped from "../../assests/slideClone/root_23_0_x4246380_y4095901.png";
import iconAcademic from "../../assests/slideClone/root_14_0_1_0_1_x3518608_y2104950.png";
import iconEdutainment from "../../assests/slideClone/root_14_0_1_0_0_1_x4587608_y3397002.png";
import iconReadPrint from "../../assests/slideClone/root_14_0_1_0_0_9_x5669872_y3401997.png";
import iconMakers from "../../assests/slideClone/root_14_0_1_0_0_10_x6823982_y3456770.png";
import iconInfoHub from "../../assests/slideClone/root_14_0_1_2_x8127553_y6038692.png";
import iconExtra from "../../assests/slideClone/root_14_0_1_0_0_12_x7870474_y3434673.png";

const navItems = ["Individuals", "Businesses", "Schools", "Institutions", "Governments"];

const quickLinks = [
  { label: "ACADEMIC", icon: iconAcademic, color: "#35a8ef" },
  { label: "ENRICH", icon: iconExtra, color: "#f3c12c" },
  { label: "EDUTAINMENT", icon: iconEdutainment, color: "#47b95f" },
  { label: "READ & PRINT", icon: iconReadPrint, color: "#f07d28" },
  { label: "MAKERS", icon: iconMakers, color: "#7f56d9" },
  { label: "INFO HUB", icon: iconInfoHub, color: "#f44f4f" },
];

export default function SlideCloneHome() {
  return (
    <div className="slide-home-page">
      <div className="slide-home-canvas">
        <div className="slide-top-strip" />

        <header className="slide-top-nav">
          <div className="slide-top-nav-left">
            {navItems.map((item) => (
              <span key={item}>{item}</span>
            ))}
          </div>
          <div className="slide-top-nav-right">CONTACT US</div>
        </header>

        <img className="slide-badge" src={topRightBadge} alt="Google for Education partner" />

        <div className="slide-logo-wrap">
          <img className="slide-logo-main" src={logoMain} alt="MySchool logo" />
          <img className="slide-logo-tagline" src={logoTagline} alt="MySchool tagline" />
        </div>

        <section className="slide-partner-box" aria-hidden="true">
          <div className="slide-partner-copy">
            <span>Be a partner of</span>
            <strong>AI Innovation lab</strong>
            <span>at your school</span>
          </div>
          <img src={partnerGraphic} alt="" />
        </section>

        <div className="slide-mid-band" />

        <section className="slide-action-row">
          <div className="slide-auth-buttons">
            <button type="button">Log in</button>
            <button type="button">Download App</button>
            <button type="button">Sign up</button>
          </div>
          <div className="slide-search-pill" role="search" aria-label="Search your wish">
            <span className="slide-search-icon">⌕</span>
            <span>Search your wish</span>
          </div>
        </section>

        <div className="slide-left-arrow" aria-hidden="true" />

        <div className="slide-card card-far-left" aria-hidden="true">
          <img src={cardFarLeft} alt="" />
          <div className="slide-label card-far-left-label">
            <strong className="accent-blue">AI</strong>
            <span>ENABLED</span>
            <span>VOICE &amp; VIDEO LESSONS</span>
          </div>
        </div>

        <div className="slide-card card-mapped" aria-hidden="true">
          <img src={cardMapped} alt="" />
          <div className="slide-label slide-chip chip-white card-mapped-label">
            <span>FLN MAPPED</span>
            <strong>PRINTABLES</strong>
          </div>
        </div>

        <div className="slide-card card-setup" aria-hidden="true">
          <div className="setup-grid">
            <span>S</span>
            <span>E</span>
            <span>T</span>
            <span>U</span>
          </div>
          <div className="slide-label card-setup-label">
            <span>AI POWERED</span>
            <strong>LMS &amp; ERP</strong>
          </div>
        </div>

        <div className="slide-card card-left-bottom" aria-hidden="true">
          <img src={cardLeftBottom} alt="" />
          <div className="slide-label slide-chip chip-white card-left-bottom-label">
            <span>PRINT RESOURCES</span>
            <strong>FREE FOR TEACHERS</strong>
          </div>
        </div>

        <div className="slide-card card-top" aria-hidden="true">
          <img src={cardTop} alt="" />
        </div>

        <div className="slide-skill-tag slide-chip chip-maroon" aria-hidden="true">
          <span>CHECK YOUR</span>
          <strong className="accent-gold">SKILLS</strong>
          <span>AND</span>
          <strong className="accent-gold">UPGRADE</strong>
          <span>EVERY MONTH</span>
        </div>

        <div className="slide-card card-bottom" aria-hidden="true">
          <img src={cardBottom} alt="" />
        </div>

        <div className="slide-classwise-tag slide-chip chip-ice" aria-hidden="true">
          <strong>GET CLASS-WISE</strong>
          <span className="accent-red">CUT &amp; PASTE</span>
          <span>PROJECT RESOURCES EVERY MONTH</span>
        </div>

        <section className="slide-hero" aria-hidden="true">
          <img src={heroBanner} alt="" />
        </section>

        <div className="slide-hero-copy" aria-hidden="true">
          <span>CHILDREN AT MYSCHOOL EMBRACE</span>
          <span>EVERY MOMENT THROUGH</span>
          <strong>INNOVATION</strong>
        </div>

        <nav className="slide-footer-links" aria-label="Footer links">
          <span>ABOUT US</span>
          <span>SERVICES</span>
          <span>GET INVOLVED</span>
          <span>RESOURCES</span>
          <span>TESTIMONIALS</span>
        </nav>

        <div className="slide-quick-links" aria-label="Categories">
          {quickLinks.map((item) => (
            <div className="quick-link" key={item.label}>
              <div className="quick-link-icon" style={{ backgroundColor: item.color }}>
                <img src={item.icon} alt="" />
              </div>
              <span>{item.label}</span>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}
