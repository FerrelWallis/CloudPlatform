open A,$ARGV[0];	open B,$ARGV[1];

my %inf;
while(<A>){
	$inf{$_}=1;
}
close A;
while(<B>){
	if(!exists $inf{$_}){
		print "$_";
	}
}
close B;
