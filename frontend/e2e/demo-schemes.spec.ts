import {test,expect} from '@playwright/test';

const cases = [
 {name:'Student Education Support',benefit:'Rs 12,000/year',answers:{'Age (integer years)':'18','State of residence':'TN','Currently enrolled in college':true,'Annual family income (decimal INR)':'300000'},bad:'Currently enrolled in college'},
 {name:'Women Higher Education Grant',benefit:'Rs 15,000/year',answers:{Gender:'female','Age (integer years)':'30','Higher education enrollment':true,'Annual family income (decimal INR)':'400000'},bad:'Higher education enrollment'},
 {name:'Farmer Family Assistance',benefit:'Rs 8,000/year',answers:{Occupation:'farmer','Age (integer years)':'21','State of residence':'TN','Annual family income (decimal INR)':'250000'},bad:'Occupation'},
 {name:'Senior Citizen Welfare Support',benefit:'Rs 1,500/month',answers:{'Age (integer years)':'60','State of residence':'TN','Annual family income (decimal INR)':'200000'},bad:'State of residence'},
 {name:'Youth Skill Development Grant',benefit:'Rs 10,000 one-time',answers:{'Age (integer years)':'35','Currently unemployed':true,'Skill training enrollment':true},bad:'Currently unemployed'},
 {name:'Disability Education Assistance',benefit:'Rs 20,000/year',answers:{'Certified disability percentage (0-100)':'40','Age (integer years)':'16','Currently studying':true},bad:'Currently studying'},
 {name:'Small Business Startup Support',benefit:'Rs 25,000 one-time',answers:{'Age (integer years)':'45','First-time entrepreneur':true,'Annual family income (decimal INR)':'500000'},bad:'First-time entrepreneur'},
 {name:'School Excellence Scholarship',benefit:'Rs 6,000/year',answers:{'Current class':'12','Previous exam percentage (0-100)':'75','State of residence':'TN','Annual family income (decimal INR)':'300000'},bad:'Current class'},
];
for(const item of cases) test(`PDF demo: ${item.name}`,async({page})=>{
 const errors:string[]=[];page.on('pageerror',e=>errors.push(e.message));
 await page.goto('/');await page.getByRole('button',{name:'Create a citizen account'}).click();
 await page.getByLabel('Full name').fill('PDF Demo Citizen');await page.getByLabel('Email',{exact:true}).fill(`pdf-${Date.now()}@example.test`);await page.getByLabel('Password',{exact:true}).fill('demo-test-password123');await page.getByRole('button',{name:'Create account',exact:true}).click();
 await expect(page.getByRole('heading',{name:'Welcome, PDF Demo Citizen'})).toBeVisible();
 await page.getByRole('navigation',{name:'Main navigation'}).getByRole('link',{name:'Browse schemes',exact:true}).click();
 await expect(page.getByText('Synthetic Demo: Learning Support',{exact:true})).toHaveCount(0);
 await page.getByLabel('Search schemes').fill(item.name);await page.getByRole('link',{name:'View scheme',exact:true}).click();
 await expect(page.getByRole('heading',{name:`FICTIONAL / DEMO ONLY: ${item.name}`,exact:true})).toBeVisible();await expect(page.getByText(item.benefit,{exact:false})).toBeVisible();await expect(page.getByText('Required documents (display-only)',{exact:false})).toBeVisible();
 for(const [name,value] of Object.entries(item.answers)){
  const field=page.getByLabel('Demo: '+name,{exact:true});if(await field.evaluate(el=>el.tagName==='SELECT'))await field.selectOption(String(value));else await field.fill(String(value));
 }
 const save=async()=>{await page.getByRole('button',{name:'Save answers',exact:true}).click();await expect(page.getByRole('status').filter({hasText:'Answers saved'})).toBeVisible();};
 const check=async()=>{await page.getByRole('button',{name:'Check eligibility',exact:true}).click();};
 await save();await check();await expect(page.getByText('ELIGIBLE',{exact:true})).toBeVisible();await expect(page.locator('tbody tr')).toHaveCount(Object.keys(item.answers).length);
 await page.getByRole('navigation',{name:'Main navigation'}).getByRole('link',{name:'Browse schemes',exact:true}).click();await page.getByLabel('Search schemes').fill(item.name);await page.getByRole('link',{name:'View scheme',exact:true}).click();
 const bad=page.getByLabel('Demo: '+item.bad,{exact:true});await bad.selectOption(item.bad==='State of residence'?'MH':item.bad==='Occupation'||item.bad==='Current class'?'other':'false');await save();await check();await expect(page.getByText('NOT ELIGIBLE',{exact:true})).toBeVisible();
 await page.getByRole('navigation',{name:'Main navigation'}).getByRole('link',{name:'Browse schemes',exact:true}).click();await page.getByLabel('Search schemes').fill(item.name);await page.getByRole('link',{name:'View scheme',exact:true}).click();
 await page.getByLabel('Demo: '+item.bad,{exact:true}).selectOption('');await save();await check();await expect(page.getByRole('alert')).toContainText('Complete the missing');
 await page.getByRole('navigation',{name:'Main navigation'}).getByRole('link',{name:'Check history',exact:true}).click();await expect(page.getByRole('link',{name:'View explanation'})).toHaveCount(2);expect(errors).toEqual([]);
});
