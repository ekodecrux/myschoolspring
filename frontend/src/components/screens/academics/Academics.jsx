import React, { useEffect, useState } from 'react'
import { useSelector, useDispatch } from 'react-redux'
import FormatFilter from '../../../uicomponent/filter/FormatFilter'
import ImageRenderer from '../../auth/views/imageRenderer/ImageRenderer'
import './Academics.css'
import '../../../uicomponent/filter/AcademicFilter.css'
import { loadImages } from '../../../redux/apiSlice'
import { useLocation, useParams } from 'react-router-dom'
import { isMobile } from 'react-device-detect'
import SelectImage from './selectImage'
import { Modal, Chip, Box, Typography } from '@mui/material'
import { Login } from '../../auth/login/Login'
import { SignUp } from '../../auth/signUp/SignUp'
import { SearchImages } from '../../../redux/fetchSearchSlice';
import AcademicFilter from '../../../uicomponent/filter/AcademicFilter'
import { clearSelectedLabel } from '../../../redux/selectedLabelSlice'

const Academics = (props, state) => {
    const { list, continuationToken, loading, filters: apiFilters, filterField, totalCount, isTruncated } = useSelector(state => state.api)
    const selectedLabel = useSelector(state => state.selectedLabel)
    const [data, setData] = useState([])
    const [tab, setTab] = useState(0)
    const [open, setOpen] = useState(false)
    const [noSelectImage, setNoSelectImage] = useState([]);
    // State for File format filters 
    const [formats, setFormat] = useState(null)        // formats of files like png , jpg, gif, pdf etc
    const [docType, setDocType] = useState(null)      // state for type of file document like Images, Videos, Animations, etc
    const [currentSubject, setCurrentSubject] = useState(null) // Track selected subject for Academic filter
    const [selectedFilter, setSelectedFilter] = useState('ALL') // Hierarchical filter selection
    const [currentFilterPath, setCurrentFilterPath] = useState('') // Track current filter path
    const [filterBreadcrumb, setFilterBreadcrumb] = useState([]) // Track filter navigation history
    const dispatch = useDispatch()
    const location = useLocation()
    const params = useParams()
    
    // check if accessed from Academic menu
    const isAccessedFromAcademic = () => {
        const pathname = location.pathname.toLowerCase();
        return pathname.includes('/academic/');
    };
    
    // check class page
    const isAcademicClassPage = () => {
        const pathname = location.pathname.toLowerCase();
        return pathname.includes('/academic/class/') && 
               !pathname.includes('imagebank') && 
               !pathname.includes('image-bank');
    };
    
    // Map URL paths to correct folder paths
    const getAcademicPath = () => {
        const pathname = location.pathname.toLowerCase();
        const parts = pathname.split('/').filter(p => p);
        
        // Find 'academic' index to get the menu items after it
        const academicIndex = parts.findIndex(p => p.toLowerCase() === 'academic');
        const menuParts = academicIndex !== -1 ? parts.slice(academicIndex + 1) : [];
        
        // If no menu parts, fetch ALL academic content (both CLASS and IMAGE BANK)
        if (menuParts.length === 0) {
            return 'ACADEMIC';
        }
        
        const firstPart = menuParts[0]?.toLowerCase();
        
        // Handle CLASS paths - /views/academic/class/class-1/lovable-stories
        // Keep CLASS-X as is, but convert hyphens to spaces for subject paths after class level
        if (firstPart === 'class') {
            const processedParts = menuParts.map((p, i) => {
                const upper = p.toUpperCase();
                // Keep CLASS and CLASS-X (CLASS-1, CLASS-2, etc.) as is
                if (upper === 'CLASS' || upper.startsWith('CLASS-')) {
                    return upper;
                }
                // For subject paths after class level, convert hyphens to spaces
                return upper.replace(/-/g, ' ');
            });
            return 'ACADEMIC/' + processedParts.join('/');
        }
        
        // Handle IMAGE BANK paths - /views/academic/imagebank/animals
        if (firstPart === 'imagebank' || firstPart === 'image-bank') {
            if (menuParts.length > 1) {
                let category = decodeURIComponent(menuParts[1]).replace(/-/g, ' ').toUpperCase();
                
                // Special mappings for categories with different names in URL vs R2
                const categoryMappings = {
                    'AREAS OR LOCALITIES': 'AREAS OR LOCATIES',
                    'HOUSEHOLD THINGS': 'HOUSE HOLD THINGS',
                    'HOUSEHOLDTHINGS': 'HOUSE HOLD THINGS'
                };
                
                const mappedCategory = categoryMappings[category] || category;
                
                // sub-category check
                if (menuParts.length > 2) {
                    const subCategory = decodeURIComponent(menuParts[2]).replace(/-/g, ' ').toUpperCase();
                    return `ACADEMIC/IMAGE BANK/${mappedCategory}/${subCategory}`;
                }
                
                return `ACADEMIC/IMAGE BANK/${mappedCategory}`;
            }
            return 'ACADEMIC/IMAGE BANK';
        }
        
        // Handle OFFERS - /views/academic/offers
        if (firstPart === 'offers') {
            return 'OFFERS';
        }
        
        // Handle One Click Resource Center items accessed from Academic menu
        const oneClickMappings = {
            'visual-worksheets': 'ONE_CLICK_RESOURCE_CENTRE/VISUAL WORKSHEETS',
            'rhymes': 'ONE_CLICK_RESOURCE_CENTRE/RHYMES',
            'safety': 'ONE_CLICK_RESOURCE_CENTRE/SAFETY',
            'value-education': 'ONE_CLICK_RESOURCE_CENTRE/VALUE EDUCATION',
            'art-lessons': 'ONE_CLICK_RESOURCE_CENTRE/ART LESSONS',
            'craft-lessons': 'ONE_CLICK_RESOURCE_CENTRE/CRAFT LESSONS',
            'picture-stories': 'ONE_CLICK_RESOURCE_CENTRE/PICTORIAL STORIES',
            'pictorial-stories': 'ONE_CLICK_RESOURCE_CENTRE/PICTORIAL STORIES',
            'moral-stories': 'ONE_CLICK_RESOURCE_CENTRE/MORAL STORIES',
            'comics': 'ONE_CLICK_RESOURCE_CENTRE/COMICS',
            'flash-cards': 'ONE_CLICK_RESOURCE_CENTRE/FLASH CARDS',
            'gk-science': 'ONE_CLICK_RESOURCE_CENTRE/GK & SCIENCE',
            'learn-hand-writing': 'ONE_CLICK_RESOURCE_CENTRE/LEARN HAND WRITING',
            'project-charts': 'ONE_CLICK_RESOURCE_CENTRE/PROJECT CHARTS',
            'puzzles-riddles': 'ONE_CLICK_RESOURCE_CENTRE/PUZZELS & RIDDLES',
            'computer-lessons': 'ONE_CLICK_RESOURCE_CENTRE/COMPUTER LESSONS',
            'donors': 'DONORS'
        };
        
        if (oneClickMappings[firstPart]) {
            return oneClickMappings[firstPart];
        }
        
        // Default: Build path from URL parts
        // This handles any other academic subpaths like /views/academic/some-menu/some-submenu
        return 'ACADEMIC/' + menuParts.map(p => p.toUpperCase().replace(/-/g, ' ')).join('/');
    };
    
    // Function to handle loading of images
    const handleFetchImages = (folderPath) => {
        if (location?.search.includes('?text=') && location?.search.replace('?text=', "").toLowerCase() !== "") {
            let header = {
                "Content-Type": "application/json",
            }
            dispatch(SearchImages({
                headers: header,
                method: "GET",
                body: {
                    query: decodeURIComponent(location?.search.replace('?text=', "").toLowerCase()),
                    size : 100
                }
            })).then(async (res) => {
                // Transform results array to object format expected by ImageRenderer
                // ImageRenderer expects { "path/to/file.jpg": "full_url", ... }
                const results = res.payload?.data || [];
                const transformedData = {};
                results.forEach((item) => {
                    const key = item.path || item.title || '';
                    if (key) {
                        transformedData[key] = item.path || item.thumbnail || '';
                    }
                });
                setData(transformedData)
            })
            return
        }
        let path = folderPath ? folderPath : getAcademicPath();
        let header = {
            "Content-Type": "application/json"
        }
        dispatch(loadImages({
            url: "/rest/images/fetch",
            header: header,
            method: "post",
            body: { folderPath: path, imagesPerPage: 500 }
        }));
    }
    useEffect(() => handleFetchImages(), [location.pathname, location?.search.includes('?text=') ? location?.search.replace('?text=', "").toLowerCase() : null])
    const selectPicture = (data, remove) => {
        if (remove) {
            setNoSelectImage(current => {
                const copy = [...current]
                let index = copy.indexOf(data)
                if (index >= -1) {
                    copy.splice(index, 1)
                }
                return copy
            })
        } else {
            setNoSelectImage(current => {
                const copy = [...current]
                copy.push(data)
                return copy
            })
        }
    }
    const handleAddToFavourite = (data) => {
        setNoSelectImage(current => {
            const copy = [...current]
            let index = copy.indexOf(data)
            if (index > -1) {
                copy[index].markFav = !copy[index]?.markFav
            }
            return copy
        })
    }
    const handleRemoveAll = () => {
        setNoSelectImage([])
    }
    // Function handle close of Login or Sign Up
    // Modal
    const handleClose = () => {
        setOpen(false);
        setTab(0);
    };
    
    // Handle subject selection from StructureFilter (for Academic class pages)
    const handleSubjectSelect = (subject) => {
        setCurrentSubject(subject);
    };
    
    // Get base path for Image Bank - depends on which section we're in
    const getImageBankBasePath = () => {
        const pathname = location.pathname.toLowerCase();
        // Academic Image Bank uses ACADEMIC/IMAGE BANK path
        if (pathname.includes('/academic/imagebank')) {
            // Extract category from URL if present (e.g., /academic/imagebank/animals -> ANIMALS)
            const parts = pathname.split('/');
            const imagebankIndex = parts.indexOf('imagebank');
            if (imagebankIndex !== -1 && parts[imagebankIndex + 1]) {
                const category = parts[imagebankIndex + 1].replace(/-/g, ' ').toUpperCase();
                return `ACADEMIC/IMAGE BANK/${category}`;
            }
            return 'ACADEMIC/IMAGE BANK';
        }
        // Sections/One-click Image Bank uses one_click_resource_centre/image bank
        return 'one_click_resource_centre/image bank';
    };
    
    // Handle hierarchical filter selection for Image Bank - MULTI LEVEL (append)
    const handleFilterClick = (filter) => {
        const basePath = getImageBankBasePath();
        
        if (filter === 'ALL') {
            // Reset to base path - show all items in category
            setCurrentFilterPath('');
            setFilterBreadcrumb([]);
            setSelectedFilter('ALL');
            handleFetchImages(basePath);
        } else {
            // Append filter to existing breadcrumb for multi-level navigation
            const newBreadcrumb = [...filterBreadcrumb, filter];
            const newPath = `${basePath}/${newBreadcrumb.join('/')}`;
            
            setFilterBreadcrumb(newBreadcrumb);
            setCurrentFilterPath(newPath);
            setSelectedFilter(filter);
            handleFetchImages(newPath);
        }
    };
    
    // Handle breadcrumb click - go back to specific level
    const handleBreadcrumbClick = (index) => {
        const basePath = getImageBankBasePath();
        
        if (index === -1) {
            // Click on "ALL" - reset everything
            setCurrentFilterPath('');
            setFilterBreadcrumb([]);
            setSelectedFilter('ALL');
            handleFetchImages(basePath);
        } else {
            // Go back to specific level
            const newBreadcrumb = filterBreadcrumb.slice(0, index + 1);
            const newPath = `${basePath}/${newBreadcrumb.join('/')}`;
            setFilterBreadcrumb(newBreadcrumb);
            setCurrentFilterPath(newPath);
            setSelectedFilter(newBreadcrumb[newBreadcrumb.length - 1]);
            handleFetchImages(newPath);
        }
    };
    
    // image bank page check
    const isImageBankPage = () => {
        return location.pathname.toLowerCase().includes('imagebank');
    };
    
    // Reset filter when path changes
    useEffect(() => {
        setSelectedFilter('ALL');
        setCurrentFilterPath('');
        setFilterBreadcrumb([]);
    }, [location.pathname]);
    
    return (
        <>
            <div className='checkboxContainer'>
                <SelectImage
                    image={noSelectImage}
                    openLogin={setOpen}
                    handleRemove={selectPicture}
                    handleRemoveAll={handleRemoveAll}
                    handleAddToFavourite={handleAddToFavourite} />
            </div>
            <div >
                {/* Academic Class pages: Use AcademicFilter for subject/book type filtering 
                    Image Bank: Uses MegaMenu dropdown (rendered by Header.jsx) - no separate filter bar needed */}
                {!isImageBankPage() && (
                    <AcademicFilter loadImages={handleFetchImages} />
                )}
                
                {/* Showing label: visible above image grid for Academic class pages */}
                {isAcademicClassPage() && (() => {
                    // Get class from URL
                    const parts = location.pathname.toLowerCase().split('/').filter(p => p);
                    const classIndex = parts.indexOf('class');
                    const classLabel = classIndex !== -1 && parts[classIndex + 1]
                        ? `Class ${parts[classIndex + 1].replace('class-', '').toUpperCase()}`
                        : null;
                    // Get subject, bookType, lesson from Redux (set by Menubar/ItemsContainer/AcademicFilter)
                    const subjectLabel = selectedLabel?.subject
                        ? selectedLabel.subject.toUpperCase()
                        : null;
                    const bookTypeLabel = selectedLabel?.bookType
                        ? selectedLabel.bookType.replace(/_/g, ' ').replace(/\b\w/g, c => c.toUpperCase())
                        : null;
                    const lessonLabel = selectedLabel?.lesson
                        ? selectedLabel.lesson
                        : null;
                    const crumbs = [classLabel, subjectLabel, bookTypeLabel, lessonLabel].filter(Boolean);
                    if (crumbs.length < 2) return null;
                    return (
                        <Box sx={{ px: 2, py: 1, display: 'flex', alignItems: 'center', gap: 0.5, flexWrap: 'wrap' }}>
                            <Typography variant="body2" color="text.secondary" sx={{ fontWeight: 500 }}>
                                Showing:
                            </Typography>
                            {crumbs.map((crumb, idx) => (
                                <React.Fragment key={idx}>
                                    <Typography variant="body2" color={idx === crumbs.length - 1 ? 'primary' : 'text.secondary'} sx={{ fontWeight: idx === crumbs.length - 1 ? 600 : 400 }}>
                                        {crumb}
                                    </Typography>
                                    {idx < crumbs.length - 1 && <Typography variant="body2" color="text.secondary">›</Typography>}
                                </React.Fragment>
                            ))}
                        </Box>
                    );
                })()}
                <div className='academicsContainer'>
                    <div className={isMobile ? 'mobGutter' : 'homeGutter'} />
                    <div className='academicsWrapper'>
                        <FormatFilter
                            formats={formats}
                            setFormat={setFormat}
                            docType={docType}
                            setDocType={setDocType}
                        />
                        <ImageRenderer
                            openLogin={setOpen}
                            loading={loading}
                            pageType={location?.search.includes('?text=') ? "result" : 'sections'}
                            data={location?.search.includes('?text=') ? data : list}
                            token={continuationToken}
                            addedImages={noSelectImage}
                            selectedFilters={formats}
                            addToNoSelectImage={selectPicture}
                            totalCount={totalCount}
                            isTruncated={isTruncated} />
                    </div>
                    <div className={isMobile ? 'mobGutter' : 'homeGutter'} />
                </div>
            </div>
            {/* Render Modal for the Auth Section */}
            <Modal className='openModal' onClose={handleClose} open={open}>
                {tab === 0 ? (
                    <Login changeTab={() => setTab(tab === 0 ? 1 : 0)} handleCloseModal={handleClose} />
                ) : (
                    <SignUp changeTab={() => setTab(tab === 0 ? 1 : 0)} handleCloseModal={handleClose} />
                )}
            </Modal>
        </>
    )
}
export default Academics