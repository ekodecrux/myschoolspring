import React, { useState, useEffect, useCallback } from 'react'
import { ScrollMenu, VisibilityContext } from "react-horizontal-scrolling-menu";
import { LeftArrow, RightArrow } from './Arrow';
import useDrag from '../../customTheme/signUpMenu/useDrag';
import "../../customTheme/signUpMenu/hideScrollBar.css";
import { useLocation } from "react-router-dom";
import './AcademicFilter.css';

const AcademicFilter = ({ loadImages }) => {
    const location = useLocation();
    const [bookTypes, setBookTypes] = useState([]);
    const [unitLessons, setUnitLessons] = useState([]);
    const [selectedBookType, setSelectedBookType] = useState(null);
    const [selectedLesson, setSelectedLesson] = useState(null);
    const [loading, setLoading] = useState(false);
    const [isCollapsed, setIsCollapsed] = useState(false);

    const { dragStart, dragStop, dragMove, dragging } = useDrag();
    const handleDrag = ({ scrollContainer }) => (e) => dragMove(e, (posDiff) => {
        if (scrollContainer.current) {
            scrollContainer.current.scrollLeft += posDiff;
        }
    });

    // Get current class from URL e.g. /academic/class/class-1 -> CLASS-1
    const getCurrentClass = useCallback(() => {
        const parts = location.pathname.toLowerCase().split('/').filter(p => p);
        if (parts.includes('academic') && parts.includes('class')) {
            const classIndex = parts.indexOf('class');
            if (classIndex !== -1 && parts[classIndex + 1]) {
                return parts[classIndex + 1].toUpperCase();
            }
        }
        return null;
    }, [location.pathname]);

    // Get current subject from URL e.g. /academic/class/class-1/english -> ENGLISH
    const getCurrentSubjectFromUrl = useCallback(() => {
        const parts = location.pathname.toLowerCase().split('/').filter(p => p);
        if (parts.includes('academic') && parts.includes('class')) {
            const classIndex = parts.indexOf('class');
            if (classIndex !== -1 && parts[classIndex + 2]) {
                return decodeURIComponent(parts[classIndex + 2]).toUpperCase().replace(/-/g, ' ');
            }
        }
        return null;
    }, [location.pathname]);

    // Get current book type from URL e.g. /academic/class/class-1/english/course_book -> COURSE_BOOK
    const getCurrentBookTypeFromUrl = useCallback(() => {
        const parts = location.pathname.toLowerCase().split('/').filter(p => p);
        if (parts.includes('academic') && parts.includes('class')) {
            const classIndex = parts.indexOf('class');
            if (classIndex !== -1 && parts[classIndex + 3]) {
                return decodeURIComponent(parts[classIndex + 3]).toUpperCase().replace(/-/g, '_');
            }
        }
        return null;
    }, [location.pathname]);

    // Fetch book types when subject changes (from URL)
    useEffect(() => {
        const currentClass = getCurrentClass();
        const currentSubject = getCurrentSubjectFromUrl();

        if (!currentClass || !currentSubject) {
            setBookTypes([]);
            setUnitLessons([]);
            setSelectedBookType(null);
            setSelectedLesson(null);
            return;
        }

        const fetchBookTypes = async () => {
            setLoading(true);
            try {
                const backendUrl = process.env.REACT_APP_BACKEND_URL;
                const response = await fetch(`${backendUrl}/api/rest/academic/book-types`, {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({
                        class_level: currentClass,
                        subject: currentSubject
                    })
                });

                if (response.ok) {
                    const data = await response.json();
                    const types = data.book_types || [];
                    setBookTypes(types);

                    // Check if book type is already in URL
                    const urlBookType = getCurrentBookTypeFromUrl();
                    if (urlBookType && types.includes(urlBookType)) {
                        setSelectedBookType(urlBookType);
                    } else if (types.length > 0) {
                        // Auto-select first book type and load its images
                        setSelectedBookType(types[0]);
                        loadImages(`academic/class/${currentClass.toLowerCase()}/${currentSubject.toLowerCase()}/${types[0]}`);
                    }
                }
            } catch (error) {
                console.error('Error fetching book types:', error);
            }
            setLoading(false);
        };

        fetchBookTypes();
        setSelectedLesson(null);
        setUnitLessons([]);
    // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [location.pathname]);

    // Fetch unit lessons when book type is selected
    useEffect(() => {
        const currentClass = getCurrentClass();
        const currentSubject = getCurrentSubjectFromUrl();

        if (!currentClass || !currentSubject || !selectedBookType) {
            setUnitLessons([]);
            return;
        }

        const fetchUnitLessons = async () => {
            setLoading(true);
            try {
                const backendUrl = process.env.REACT_APP_BACKEND_URL;
                const response = await fetch(`${backendUrl}/api/rest/academic/unit-lessons`, {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({
                        class_level: currentClass,
                        subject: currentSubject,
                        book_type: selectedBookType
                    })
                });

                if (response.ok) {
                    const data = await response.json();
                    const sortedLessons = (data.unit_lessons || []).sort((a, b) => {
                        const numA = parseInt(a.match(/^(\d+)/)?.[1] || '999');
                        const numB = parseInt(b.match(/^(\d+)/)?.[1] || '999');
                        return numA - numB;
                    });
                    setUnitLessons(sortedLessons);
                }
            } catch (error) {
                console.error('Error fetching unit lessons:', error);
            }
            setLoading(false);
        };

        fetchUnitLessons();
        // Load images for the selected book type
        loadImages(`academic/class/${currentClass.toLowerCase()}/${currentSubject.toLowerCase()}/${selectedBookType}`);
    // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [selectedBookType]);

    const handleBookTypeClick = (bookType) => {
        if (dragging) return;
        setSelectedBookType(bookType);
        setSelectedLesson(null);
    };

    const handleLessonClick = (lesson) => {
        if (dragging) return;
        setSelectedLesson(lesson);

        const currentClass = getCurrentClass();
        const currentSubject = getCurrentSubjectFromUrl();
        if (currentClass && currentSubject && selectedBookType) {
            loadImages(`academic/class/${currentClass.toLowerCase()}/${currentSubject.toLowerCase()}/${selectedBookType}/${lesson}`);
        }
    };

    const formatBookTypeName = (bookType) => {
        return bookType.replace(/_/g, ' ').toLowerCase().replace(/\b\w/g, c => c.toUpperCase());
    };

    const currentClass = getCurrentClass();
    const currentSubject = getCurrentSubjectFromUrl();

    // Only render when we are on a subject page (class + subject in URL)
    if (!currentClass || !currentSubject) {
        return null;
    }

    // Build breadcrumb path
    const getBreadcrumb = () => {
        const parts = [`Class ${currentClass.replace('CLASS-', '')}`];
        if (currentSubject) parts.push(currentSubject);
        if (selectedBookType) parts.push(formatBookTypeName(selectedBookType));
        if (selectedLesson) parts.push(selectedLesson);
        return parts;
    };

    const breadcrumb = getBreadcrumb();

    return (
        <div className="academicFilterContainer">
            {/* Header bar - always visible when on a subject page */}
            <div className="filterPanelHeader">
                <span className="filterPanelSelected">
                    {breadcrumb.map((item, idx) => (
                        <span key={idx}>
                            <span
                                className={idx === breadcrumb.length - 1 ? 'breadcrumbActive' : 'breadcrumbItem'}
                                onClick={() => {
                                    if (idx === 1) {
                                        // Click subject - go back to subject level
                                        setSelectedBookType(null);
                                        setSelectedLesson(null);
                                        setUnitLessons([]);
                                        loadImages(`academic/class/${currentClass.toLowerCase()}/${currentSubject.toLowerCase()}`);
                                    } else if (idx === 2) {
                                        // Click book type - go back to book type level
                                        setSelectedLesson(null);
                                        loadImages(`academic/class/${currentClass.toLowerCase()}/${currentSubject.toLowerCase()}/${selectedBookType}`);
                                    }
                                }}
                            >
                                {item}
                            </span>
                            {idx < breadcrumb.length - 1 && <span className="breadcrumbSeparator"> › </span>}
                        </span>
                    ))}
                </span>
                <button
                    className="collapseToggle"
                    onClick={() => setIsCollapsed(!isCollapsed)}
                >
                    {isCollapsed ? '▼ Expand' : '▲ Collapse'}
                </button>
            </div>

            {/* Blue panel - book types and lessons */}
            {!isCollapsed && (
                <div className="academicBluePane">
                    {/* Book Type chips */}
                    {bookTypes.length > 0 && (
                        <div className="bookTypesRow">
                            {bookTypes.map((bookType) => (
                                <div
                                    key={bookType}
                                    className={`bookTypeChip ${selectedBookType === bookType ? 'active' : ''}`}
                                    onClick={() => handleBookTypeClick(bookType)}
                                >
                                    {formatBookTypeName(bookType)}
                                </div>
                            ))}
                        </div>
                    )}

                    {/* Unit/Lesson chips - show when book type is selected */}
                    {selectedBookType && unitLessons.length > 0 && (
                        <div className="lessonsRow">
                            <ScrollMenu
                                scrollContainerClassName='lessonScrollContainer'
                                onMouseDown={() => dragStart}
                                LeftArrow={LeftArrow}
                                RightArrow={RightArrow}
                                onMouseUp={() => dragStop}
                                onMouseMove={handleDrag}
                            >
                                {unitLessons.map((lesson) => (
                                    <LessonCard
                                        key={lesson}
                                        itemId={lesson}
                                        title={lesson}
                                        selected={selectedLesson === lesson}
                                        onClick={() => handleLessonClick(lesson)}
                                    />
                                ))}
                            </ScrollMenu>
                        </div>
                    )}
                </div>
            )}

            {loading && <div className="filterLoadingIndicator">Loading...</div>}
        </div>
    );
};

function LessonCard({ onClick, selected, title }) {
    const visibility = React.useContext(VisibilityContext);

    return (
        <div
            onClick={() => onClick(visibility)}
            className={`lessonChip ${selected ? 'selected' : ''}`}
            tabIndex={0}
        >
            {title}
        </div>
    );
}

export default AcademicFilter;
