import {beforeEach,afterEach,it,expect,vi} from 'vitest';
import {render,screen,fireEvent,waitFor,cleanup,within} from '@testing-library/react';
import '@testing-library/jest-dom/vitest';
import {api,ApiError} from './api';
import {Matching} from './main';
vi.mock('./api',async importOriginal=>({...await importOriginal<typeof import('./api')>(),api:vi.fn()}));
const mocked=vi.mocked(api);
const questions=[{id:'1',label:'Demo: Age (integer years)',type:'NUMBER',value:'21',options:[]},{id:'2',label:'Demo: Currently enrolled in college',type:'BOOLEAN',value:true,options:[]}];
const rows=['ELIGIBLE','NOT_ELIGIBLE','NEEDS_INFORMATION'].map((classification,i)=>({schemeId:String(i+11),schemeName:`FICTIONAL / DEMO ONLY: Scheme ${i}`,synthetic:true,benefits:'Rs 12,000/year - invented only',ruleVersionId:'10',classification,missingQuestions:[{fieldKey:'question:1',label:questions[0].label}],explanations:[{nodeId:'age',questionLabel:questions[0].label,operator:'GTE',expectedValue:'18',actualValue:classification==='NEEDS_INFORMATION'?null:'21',result:classification==='ELIGIBLE'?'TRUE':classification==='NOT_ELIGIBLE'?'FALSE':'UNKNOWN'}]}));
beforeEach(()=>{mocked.mockReset();mocked.mockImplementation(async(path,method='GET')=>{if(path==='/matching/questions')return{revision:'4',questions};if(path==='/matching/answers'&&method==='PUT')return{revision:'5'};if(path.startsWith('/matching/preview?'))return{profileRevision:'5',results:rows,disclaimer:'FICTIONAL / DEMO ONLY. No history is saved.'};throw new Error('Unexpected request '+path);});});
afterEach(cleanup);
it('loads saved shared answers and previews without a write or individual evaluation',async()=>{
 render(<Matching/>);await screen.findByLabelText(questions[0].label.replace('Demo: ',''));await waitFor(()=>expect(screen.getByLabelText(questions[0].label.replace('Demo: ',''))).toHaveValue(21));expect(screen.getByLabelText(questions[1].label.replace('Demo: ',''))).toHaveValue('true');
 fireEvent.click(screen.getByRole('button',{name:'Preview saved answers'}));const results=await screen.findByRole('region',{name:'Matching results'});
 expect(within(results).getByText('ELIGIBLE',{exact:true})).toBeInTheDocument();expect(within(results).getByText('NOT ELIGIBLE',{exact:true})).toBeInTheDocument();expect(within(results).getByText('NEEDS INFORMATION',{exact:true})).toBeInTheDocument();expect(within(results).getByText('Missing questions:')).toBeInTheDocument();expect(within(results).getAllByRole('link',{name:'View scheme and check individually'})).toHaveLength(3);
 expect(mocked.mock.calls.every(([,method])=>!method||method==='GET')).toBe(true);expect(mocked).toHaveBeenCalledWith('/matching/preview?expectedProfileRevision=4');
});
it('saves exact strings once, previews the returned revision and clears obsolete results on edit',async()=>{
 render(<Matching/>);await screen.findByLabelText(questions[0].label.replace('Demo: ',''));fireEvent.change(screen.getByLabelText(questions[0].label.replace('Demo: ','')),{target:{value:'25'}});expect(screen.getByRole('button',{name:'Preview saved answers'})).toBeDisabled();
 fireEvent.click(screen.getByRole('button',{name:'Save answers and match'}));await screen.findByRole('region',{name:'Matching results'});
 expect(mocked).toHaveBeenCalledWith('/matching/answers','PUT',{expectedProfileRevision:'4',answers:[{questionId:'1',value:'25'},{questionId:'2',value:true}]});expect(mocked).toHaveBeenCalledWith('/matching/preview?expectedProfileRevision=5');expect(mocked.mock.calls.filter(([,method])=>method==='PUT')).toHaveLength(1);
 fireEvent.change(screen.getByLabelText(questions[0].label.replace('Demo: ','')),{target:{value:''}});expect(screen.queryByRole('region',{name:'Matching results'})).not.toBeInTheDocument();expect(screen.getByRole('button',{name:'Preview saved answers'})).toBeDisabled();
});
it('reports conflicts without retrying, retains edits and does not preview failed saves',async()=>{
 mocked.mockImplementation(async(path,method='GET')=>{if(path==='/matching/questions')return{revision:'4',questions};if(method==='PUT')throw new ApiError(409,'STALE_REVISION','Data changed.');throw new Error(path);});
 render(<Matching/>);await screen.findByLabelText(questions[0].label.replace('Demo: ',''));fireEvent.change(screen.getByLabelText(questions[0].label.replace('Demo: ','')),{target:{value:'22'}});fireEvent.click(screen.getByRole('button',{name:'Save answers and match'}));expect(await screen.findByRole('alert')).toHaveTextContent('Reload');expect(screen.getByLabelText(questions[0].label.replace('Demo: ',''))).toHaveValue(22);expect(mocked.mock.calls.filter(([,method])=>method==='PUT')).toHaveLength(1);expect(mocked.mock.calls.some(([path])=>path.startsWith('/matching/preview'))).toBe(false);
});
