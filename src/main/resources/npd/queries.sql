select * from licence
limit 100;

select c.cmpshortname
from company c
where c.cmpLongName not in (
	select c2.cmpLongName
	from company c2 join field on c2.cmplongname = field.cmplongname
)
order by c.cmpShortName;


select c.cmpshortname, c.cmpLongName
from company c
where c.cmpShortName='4SEA ENERGY AS'