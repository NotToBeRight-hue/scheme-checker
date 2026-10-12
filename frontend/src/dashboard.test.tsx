import {beforeEach,afterEach,it,expect,vi} from 'vitest';
import {render,screen,within,cleanup} from '@testing-library/react';
import '@testing-library/jest-dom/vitest';
import {api} from './api';
import {Dashboard} from './main';
vi.mock('./api',async importOriginal=>({...await importOriginal<typeof import('./api')>(),api:vi.fn()}));
const mocked=vi.mocked(api);
const records=Array.from({length:8},(_,i)=>({id:String(11+i),name:`FICTIONAL / DEMO ONLY: Stored scheme ${i}`,categoryId:'2',description:'Stored description',benefits:'Rs 100/year',synthetic:true,publishedVersionId:String(10+i),sourceMapping:'{"demoSpecification":"eight-demo-pdf-v1"}'}));
beforeEach(()=>{mocked.mockReset();mocked.mockImplementation(async path=>{
 if(path==='/dashboard')return{profileCompleteness:20,evaluationCount:0,pendingSuggestions:0,recentChecks:[]};if(path==='/profile')return{revision:'4',profile:{stateCode:'TN'}};if(path==='/categories')return[{id:'2',name:'Stored category'}];if(path==='/schemes?size=100')return{items:[...records,{id:'9',name:'Browser fixture',synthetic:true,sourceMapping:'{}'}]};throw new Error(path);
});});afterEach(cleanup);
it('uses authenticated and stored data, supplied hero assets, and an empty matching state without evaluating',async()=>{
 render(<Dashboard user={{id:'1',fullName:'Actual Test Citizen',email:'person@example.test'}}/>);expect(screen.getByRole('heading',{name:'Welcome, Actual Test Citizen'})).toBeInTheDocument();await screen.findByRole('heading',{name:records[0].name});expect(screen.getAllByRole('link',{name:'View scheme'})).toHaveLength(8);expect(screen.queryByText('Browser fixture')).not.toBeInTheDocument();expect(await screen.findByText('No matching preview in this session yet.')).toBeInTheDocument();expect(screen.getByAltText('Government building illustration in daylight')).toHaveAttribute('src','/assets/hero_light.png');expect(screen.getByAltText('Government building illustration at night')).toHaveAttribute('src','/assets/hero_dark.png');expect(mocked.mock.calls.every(([path])=>!path.startsWith('/matching'))).toBe(true);
});
it('shows counts from the real preview prop, actual profile fields and no fabricated activity',async()=>{
 const preview={profileRevision:'4',results:records.map((record,i)=>({schemeId:record.id,ruleVersionId:record.publishedVersionId,classification:i===3?'NOT_ELIGIBLE':'ELIGIBLE'}))};render(<Dashboard user={{id:'1',fullName:'Actual Test Citizen',email:'person@example.test'}} preview={preview}/>);const overview=await screen.findByRole('region',{name:'Eligibility overview'});await screen.findByText('Total schemes evaluated');expect(within(overview).getByText('7')).toBeInTheDocument();expect(within(overview).getByText('8')).toBeInTheDocument();expect(screen.getByText('TN',{exact:true})).toBeInTheDocument();expect(screen.getByText('No individual checks yet. Matching previews are not recorded here.')).toBeInTheDocument();
});

