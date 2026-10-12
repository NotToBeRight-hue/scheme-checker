import {it,expect} from 'vitest';
import {displayText,schemeDisplayName,schemeNames,pdfDemoSchemes,matchingOverview} from './ui-model';
const schemes=Array.from({length:8},(_,i)=>({id:String(11+i),publishedVersionId:String(10+i),synthetic:true,sourceMapping:'{"demoSpecification":"eight-demo-pdf-v1"}'}));
const preview={profileRevision:'4',results:schemes.map((scheme,i)=>({schemeId:scheme.id,ruleVersionId:scheme.publishedVersionId,classification:i===3?'NOT_ELIGIBLE':'ELIGIBLE'}))};
it('selects published PDF demo records without including the browser fixture or malformed metadata',()=>{
 expect(pdfDemoSchemes([...schemes,{id:'9',synthetic:true,sourceMapping:'{}'},{id:'20',synthetic:true,sourceMapping:'invalid'}])).toEqual(schemes);
});
it('cleans all eight names only for display and preserves unrelated record names',()=>{
 for(const name of schemeNames)expect(schemeDisplayName('FICTIONAL / DEMO ONLY: '+name)).toBe(name);
 expect(schemeDisplayName('Synthetic Browser Demo')).toBe('Synthetic Browser Demo');
});
it('removes known repetitive metadata warnings without changing amounts or document guidance',()=>{
 expect(displayText('Rs 12,000/year \u2014 invented demonstration amount only. No payout or entitlement.')).toBe('Rs 12,000/year');
 expect(displayText('Support. FICTIONAL / DEMO ONLY. Not a government programme or available financial assistance.')).toBe('Support.');
 expect(displayText('FICTIONAL / DEMO ONLY. Required documents (display-only): Age proof. Do not submit documents, Aadhaar numbers or bank details. No application or official verification occurs.')).toBe('Required documents (display-only): Age proof. Do not submit documents, Aadhaar numbers or bank details.');
});
it('derives real counts only from a current matching snapshot',()=>{
 expect(matchingOverview(null,'4',schemes)).toBeNull();expect(matchingOverview(preview,'4',schemes)).toEqual({eligible:7,notEligible:1,needsInformation:0,total:8});
 const unknown={...preview,results:preview.results.map(row=>({...row,classification:'NEEDS_INFORMATION'}))};expect(matchingOverview(unknown,'4',schemes)).toEqual({eligible:0,notEligible:0,needsInformation:8,total:8});
});
it('invalidates stale revisions, changed rules, partial results and duplicate scheme IDs',()=>{
 expect(matchingOverview(preview,'5',schemes)).toBeNull();expect(matchingOverview(preview,'4',[{...schemes[0],publishedVersionId:'99'},...schemes.slice(1)])).toBeNull();expect(matchingOverview({...preview,results:preview.results.slice(1)},'4',schemes)).toBeNull();expect(matchingOverview({...preview,results:[preview.results[0],...preview.results.slice(0,7)]},'4',schemes)).toBeNull();
});
it('preserves benefit data and fictional prefixes while displaying punctuation without em dashes',()=>{
 const value='FICTIONAL / DEMO ONLY: Rs 12,000/year \u2014 invented';expect(displayText(value)).toBe('FICTIONAL / DEMO ONLY: Rs 12,000/year ; invented');expect(value).toContain('\u2014');
});
